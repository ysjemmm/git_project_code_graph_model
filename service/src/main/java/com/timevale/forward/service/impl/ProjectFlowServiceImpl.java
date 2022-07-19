package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Objects;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.client.ProjectFlowService;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.ProjectFlowAddReq;
import com.timevale.forward.facade.api.request.ProjectFlowDocModifyReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.ProjectFlowDetailVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectFlowComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.copy.ProjectFlowCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

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
    private ProjectComponent projectComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectLogComponent projectLogComponent;

    @Resource
    private ProjectFlowComponent projectFlowComponent;

    @Resource
    private FileComponent fileComponent;

    @Value("${domain_name:http://forward-front-forward-itm-v1.projectk8s.tsign.cn/}")
    private String domainName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<String> add(ProjectFlowAddReq projectFlowAddReq) {
        log.info("发起详设评审,参数:{}", projectFlowAddReq);
        ProjectFlowDO projectFlowDO = ProjectFlowCopier.INSTANCE.convert(projectFlowAddReq);
        ProjectDO oldProjectDO = projectMapper.get(projectFlowDO.getProjectId());
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectId(projectFlowDO.getProjectId());
        if (!CollectionUtils.isEmpty(projectFlowDos)) {
            projectFlowDos.sort(Comparator.comparing(ProjectFlowDO::getCreateDate).reversed());
            ProjectFlowDO oldFlowDo = projectFlowDos.get(0);
            if (FlowStatusEnum.AUDITING.getCode().equals(oldFlowDo.getStatus())) {
                throw new BaseBizRuntimeException("详设评审正在审核中,请不要重复发起");
            }
            if (FlowStatusEnum.COMPLETE.getCode().equals(oldFlowDo.getStatus())) {
                throw new BaseBizRuntimeException("详设评审已通过,请不要重复发起");
            }
        }
        ProjectNodeDO projectNodeDo = projectNodeMapper.getByName(projectFlowDO.getProjectId(), ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText());
        if (projectNodeDo == null) {
            throw new BaseBizRuntimeException("技术详设评审节点不存在");
        }
        projectNodeMapper.updateActualDateById(projectNodeDo.getId(), null);
        //更新节点状态
        projectComponent.updateNodeStatus(projectFlowDO.getProjectId());

        projectNodeDo = projectNodeMapper.getByName(projectFlowDO.getProjectId(), ProjectNodeEnum.START_PLAN.getText());
        if (projectNodeDo == null) {
            //需求规划阶段被删除,详设评审为第一个节点,需要清空项目实际开始时间
            oldProjectDO.setActualStartDate(null);
            projectMapper.update(oldProjectDO);
        }

        Integer newStatus = projectComponent.getStatus(projectFlowDO.getProjectId());
        if (!Objects.equal(oldProjectDO.getStatus(), newStatus)
                && !ProjectStatusEnum.INVALID.getCode().equals(oldProjectDO.getStatus())
                && !ProjectStatusEnum.SUSPEND.getCode().equals(oldProjectDO.getStatus())) {
            oldProjectDO.setStatus(newStatus);
            projectMapper.update(oldProjectDO);
            // 日志处理
            projectLogComponent.addLogWhenStatusChange(oldProjectDO.getStatus(), newStatus, oldProjectDO.getId(), ButtonActionEnum.START_REVIEW.getText());
        }

        String processInstanceId = startWorkflow(projectFlowAddReq);
        projectFlowDO.setFlowId(processInstanceId);
        projectFlowDO.setStatus(FlowStatusEnum.AUDITING.getCode());
        projectFlowMapper.insert(projectFlowDO);
        return BaseResult.success(processInstanceId);
    }

    @Override
    public BaseResult<Boolean> modifyDoc(ProjectFlowDocModifyReq projectFlowDocModifyReq) {
        // TODO jingchun
        ProjectFlowDO projectFlow = projectFlowMapper.get(projectFlowDocModifyReq.getId(), null);
        if (projectFlow == null) {
            throw new BaseBizRuntimeException("您修改的项目流程不存在，请刷新后再试");
        }
        projectFlow.setReviewUrl(projectFlowDocModifyReq.getReviewUrl());
        if (CollectionUtils.isNotEmpty(projectFlowDocModifyReq.getFiles())) {
            fileComponent.update(projectFlowDocModifyReq.getFiles(), projectFlow.getId(), FileTypeEnum.TECH_REVIEW.getCode());
        }
        projectFlowMapper.update(projectFlow);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectFlowDetailVO> get(Long projectFlowId) {
        log.info("查看详设评审,参数:{}", projectFlowId);
        ProjectFlowDO oldFlowDo = projectFlowMapper.get(projectFlowId, null);
        if (oldFlowDo == null) {
            throw new BaseBizRuntimeException("找不到该审批流程");
        }

        List<String> reviewList = JSONObject.parseArray(oldFlowDo.getReview(), String.class);
        List<String> reviewIdList = JSONObject.parseArray(oldFlowDo.getReviewId(), String.class);
        List<PersonVO> reviews = new ArrayList<>();
        for (int i = 0; i < reviewList.size(); i++) {
            PersonVO personVO = new PersonVO();
            personVO.setUserName(reviewList.get(i));
            personVO.setUserId(reviewIdList.get(i));
            reviews.add(personVO);
        }

        if (FlowStatusEnum.AUDITING.getCode().equals(oldFlowDo.getStatus())) {
            projectFlowComponent.updateFlowInfo(oldFlowDo.getFlowId());
            oldFlowDo = projectFlowMapper.get(projectFlowId, null);
        }
        ProjectFlowDetailVO projectFlowDetailVO = ProjectFlowCopier.INSTANCE.convert(oldFlowDo);
        projectFlowDetailVO.setStatusName(FlowStatusEnum.getTextByCode(oldFlowDo.getStatus()));
        PersonVO proposer = new PersonVO();
        proposer.setUserName(oldFlowDo.getProposer());
        proposer.setUserId(oldFlowDo.getProposerId());
        projectFlowDetailVO.setProposerVO(proposer);
        projectFlowDetailVO.setReviews(reviews);
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectId(oldFlowDo.getProjectId());
        long count = projectFlowDos.stream().filter(a -> FlowStatusEnum.REJECT.getCode().equals(a.getStatus())).count();
        projectFlowDetailVO.setReturnCount(count);
        return BaseResult.success(projectFlowDetailVO);
    }


    public String startWorkflow(ProjectFlowAddReq projectFlowAddReq) {
        Map<String, Object> variables = new HashMap<>();
        StartProcessRequest start = new StartProcessRequest();
        variables.put("reviewUrl", projectFlowAddReq.getReviewUrl());
        variables.put("reviewDate", DateUtil.parseToString(projectFlowAddReq.getReviewDate(), DateFormatConst.DATE_FORMAT));
        variables.put("proposer", projectFlowAddReq.getProposer().getUserName());
        //评审人,表单展示
        String reviewName = projectFlowAddReq.getReviews().stream().map(PersonAddReq::getUserName).collect(Collectors.joining(","));
        variables.put("reviewName", reviewName);
        String projectName = projectMapper.get(projectFlowAddReq.getProjectId()).getName();
        variables.put("projectName", projectName);
        String baseUrl = domainName + "projectManagement/edit?id=%d&type=check";
        variables.put("projectUrl", String.format(baseUrl, projectFlowAddReq.getProjectId()));
        List<String> reviewIds = projectFlowAddReq.getReviews().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        variables.put("review", reviewIds);

        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_techReview");
        start.setStartAccountId(projectFlowAddReq.getProposer().getUserId());
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        return epeiusClient.start(start);
    }
}
