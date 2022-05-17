package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.timevale.epeius.service.model.request.StartProcessRequest;
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
import java.util.*;
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
    public BaseResult<String> add(ProjectFlowAddReq projectFlowAddReq) {
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
//        String processInstanceId = startWorkflow(projectFlowAddReq);
        projectFlowDO.setFlowId("1");
        projectFlowDO.setStatus(ProjectFlowStatusEnum.REVIEWING.getCode());
        projectFlowMapper.insert(projectFlowDO);
        fileComponent.add(projectFlowAddReq.getFiles(), projectFlowDO.getId(), FileTypeEnum.TECH_REVIEW.getCode());
        return BaseResult.success("1");
    }

    @Override
    public BaseResult<ProjectFlowDetailVO> get(Long projectFlowId) {
        log.info("查看详设评审,参数:{}", projectFlowId);
        ProjectFlowDO oldFlowDo = projectFlowMapper.get(projectFlowId,null);
        if (oldFlowDo == null) {
            throw new BaseBizRuntimeException("找不到该审批流程");
        }

        List<String> reviewList = JSONObject.parseArray(oldFlowDo.getReview(), String.class);
        List<String> reviewIdList = JSONObject.parseArray(oldFlowDo.getReviewId(), String.class);
        Map<String, PersonVO> reviewMap = new HashMap<>();
        List<PersonVO> reviews = new ArrayList<>();
        for (int i = 0; i < reviewList.size(); i++) {
            PersonVO personVO = new PersonVO();
            personVO.setUserName(reviewList.get(i));
            personVO.setUserId(reviewIdList.get(i));
            reviews.add(personVO);
            String account = StringUtils.substringBefore(reviewList.get(i), "-");
//            reviewMap.put(account, personVO);
        }

        if (ProjectFlowStatusEnum.REVIEWING.getCode().equals(oldFlowDo.getStatus())) {
            if (StringUtils.isEmpty(oldFlowDo.getFlowId())) {
                log.info("无流程id");
                return BaseResult.success();
            }
            projectFlowMapper.update(oldFlowDo);
        }
        ProjectFlowDetailVO projectFlowDetailVO = ProjectFlowCopier.INSTANCE.convert(oldFlowDo);
        projectFlowDetailVO.setStatusName(ProjectFlowStatusEnum.getTextByCode(oldFlowDo.getStatus()));
        PersonVO proposer = new PersonVO();
        proposer.setUserName(oldFlowDo.getProposer());
        proposer.setUserId(oldFlowDo.getProposerId());
        projectFlowDetailVO.setProposerVO(proposer);
        projectFlowDetailVO.setReviews(reviews);
        //附件
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectId(oldFlowDo.getProjectId());
        List<FileDO> fileDO = fileComponent.select(oldFlowDo.getId(), FileTypeEnum.TECH_REVIEW.getCode());
        projectFlowDetailVO.setFiles(FileCopier.INSTANCE.transform(fileDO));
        long count = projectFlowDos.stream().filter(a -> ProjectFlowStatusEnum.REVIEW_FAIL.getCode().equals(a.getStatus())).count();
        projectFlowDetailVO.setReturnCount(count);
        return BaseResult.success(projectFlowDetailVO);
    }


    public String startWorkflow(ProjectFlowAddReq projectFlowAddReq) {
        Map<String, Object> variables = new HashMap<>();
        StartProcessRequest start = new StartProcessRequest();
        variables.put("files", new ArrayList<>());
        variables.put("reviewUrl", projectFlowAddReq.getReviewUrl());
        variables.put("reviewDate", "2022-05-16");
        variables.put("reviewName", "星云-敖哲");
        variables.put("projectName", "ITM线上化一期"+System.currentTimeMillis());
        variables.put("projectUrl", "http://forward-front-forward-optimize-v2.projectk8s.tsign.cn/projectManagement/edit?id=351&type=check");
//        List<String> list = Arrays.asList("xingyun", "shanluo", "yangxu");
        List<String> list = Arrays.asList("xingyun",  "shanluo");
//        List<String> list = Arrays.asList("xingyun");
        variables.put("review", list);
        Map<String, String> file = new HashMap<>();
        file.put("file_key", "$fa0fb506-ef38-4826-9f0b-94a729ebfa20$1618977430");
        file.put("file_name", "产品线.png");
        file.put("download_url", "");
        List<Map<String, String>> files = new ArrayList<>();
        files.add(file);
        variables.put("files", files);
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_techReview");
        start.setStartAccountId("xingyun");
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        String processInstanceId = epeiusClient.start(start);
        System.out.println(processInstanceId);
        return processInstanceId;
    }
}
