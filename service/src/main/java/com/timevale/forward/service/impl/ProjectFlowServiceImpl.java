package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.client.ProjectFlowService;
import com.timevale.forward.facade.api.request.ProjectFlowAddReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.ProjectFlowDetailVO;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.ProjectFlowStatusEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.ProjectFlowCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectFlowServiceImpl implements ProjectFlowService {

    @Resource
    private EpeiusClient epeiusClient;

    @Resource
    private ProjectNodeMapper projectNodeMapper;

    @Resource
    private ProjectFlowMapper projectFlowMapper;

    @Resource
    private FileComponent fileComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectFlowAddReq projectFlowAddReq) {
        log.info("发起详设评审,参数:{}", projectFlowAddReq);
        ProjectFlowDO projectFlowDO = ProjectFlowCopier.INSTANCE.convert(projectFlowAddReq);
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectId(projectFlowDO.getProjectId());
        if (!CollectionUtils.isEmpty(projectFlowDos)) {
            projectFlowDos.sort(Comparator.comparing(ProjectFlowDO::getModifyDate).reversed());
            ProjectFlowDO oldFlowDo = projectFlowDos.get(0);
            if (ProjectFlowStatusEnum.REVIEWING.getCode().equals(oldFlowDo.getStatus())) {
                //            throw new BaseBizRuntimeException("详设评审正在审核中,请不要重复发起");
            }
            if (ProjectFlowStatusEnum.REVIEWED.getCode().equals(oldFlowDo.getStatus())) {
//            throw new BaseBizRuntimeException("详设评审已通过,请不要重复发起");
            }
        }
        List<ProjectNodeDO> projectNodeDos = projectNodeMapper.get(projectFlowDO.getProjectId());
        List<ProjectNodeDO> list = projectNodeDos
                .stream()
                .filter(a -> ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText().equals(a.getName())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(list)) {
            throw new BaseBizRuntimeException("该节点不存在");
        }
        ProjectNodeDO nodeDO = list.get(0);
        projectNodeMapper.updateActualDateById(nodeDO.getId(), null);
//        epeiusClient.start()
        projectFlowDO.setFlowId("1");
        projectFlowDO.setStatus(ProjectFlowStatusEnum.REVIEWING.getCode());
        projectFlowMapper.insert(projectFlowDO);
        fileComponent.add(projectFlowAddReq.getFiles(), projectFlowDO.getId(), FileTypeEnum.TECH_REVIEW.getCode());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectFlowDetailVO> get(Long pid) {
        log.info("查看详设评审,参数:{}", pid);
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectId(pid);
        if (CollectionUtils.isEmpty(projectFlowDos)) {
            return BaseResult.success();
        }
        projectFlowDos.sort(Comparator.comparing(ProjectFlowDO::getModifyDate).reversed());
        ProjectFlowDO oldFlowDo = projectFlowDos.get(0);
        if (ProjectFlowStatusEnum.REVIEWING.getCode().equals(oldFlowDo.getStatus())) {
            if(StringUtils.isEmpty(oldFlowDo.getFlowId())){
                log.info("无流程id");
                return BaseResult.success();
            }
//            List<ProcessLogResponse> responses = epeiusClient.flowLog(oldFlowDo.getFlowId());
//            responses.stream().map(ProcessLogResponse::getActivityStatus);
            //若状态终止,更新审核人员,状态信息
            projectFlowMapper.update(oldFlowDo);
        }
        ProjectFlowDetailVO projectFlowDetailVO = ProjectFlowCopier.INSTANCE.convert(oldFlowDo);
        projectFlowDetailVO.setStatusName(ProjectFlowStatusEnum.getTextByCode(oldFlowDo.getStatus()));
        PersonVO proposer=new PersonVO();
        proposer.setUserName(oldFlowDo.getProposer());
        proposer.setUserId(oldFlowDo.getProposerId());
        projectFlowDetailVO.setProposerVO(proposer);
        List<String> reviewList = JSONObject.parseArray(oldFlowDo.getReview(), String.class);
        List<String> reviewIdList = JSONObject.parseArray(oldFlowDo.getReviewId(), String.class);
        List<PersonVO> reviews=new ArrayList<>();
        for (int i = 0; i < reviewList.size(); i++) {
            PersonVO personVO=new PersonVO();
            personVO.setUserName(reviewList.get(i));
            personVO.setUserId(reviewIdList.get(i));
            reviews.add(personVO);
        }
        projectFlowDetailVO.setReviews(reviews);
        //附件
        List<FileDO> fileDO = fileComponent.select(oldFlowDo.getId(), FileTypeEnum.TECH_REVIEW.getCode());
        projectFlowDetailVO.setFiles(FileCopier.INSTANCE.transform(fileDO));
        long count = projectFlowDos.stream().filter(a -> ProjectFlowStatusEnum.REVIEW_FAIL.getCode().equals(a.getStatus())).count();
        projectFlowDetailVO.setReturnCount(count);
        return BaseResult.success(projectFlowDetailVO);
    }
}
