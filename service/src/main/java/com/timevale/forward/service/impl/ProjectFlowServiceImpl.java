package com.timevale.forward.service.impl;

import com.google.common.base.Objects;

import com.alibaba.fastjson.JSONObject;
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
import com.timevale.forward.facade.api.result.ProjectFlowNodeVO;
import com.timevale.forward.model.enums.FlowStatusEnum;
import com.timevale.forward.model.enums.MessageTagEnum;
import com.timevale.forward.model.enums.ProjectNodeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectFlowComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.copy.ProjectFlowCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.utils.PageUtil;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.base.util.DateUtils;
import com.timevale.mandarin.common.annotation.RestService;

import com.timevale.mandarin.common.query.QueryBase;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

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

    @Value("${domain_name:http://forward-front-forward-itm-v1.projectk8s.tsign.cn/}")
    private String domainName;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<String> add(ProjectFlowAddReq projectFlowAddReq) {
        log.info("发起评审,参数:{}", projectFlowAddReq);
        ProjectFlowDO projectFlowDO = ProjectFlowCopier.INSTANCE.convert(projectFlowAddReq);
        ProjectDO oldProjectDO = projectMapper.get(projectFlowDO.getProjectId());
        ProjectNodeEnum projectNodeEnum = ProjectNodeEnum.getByCode(projectFlowAddReq.getFlowType());
        if (projectNodeEnum == null) {
            throw new BaseBizRuntimeException("对应项目节点不存在，请修改后再发起");
        }

        // 需求内审、需求串讲、ued评审、详设评审可以发起
        if (!ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getCode().equals(projectNodeEnum.getCode())
                && !ProjectNodeEnum.DEMAND_CONSTRUE.getCode().equals(projectNodeEnum.getCode())
                && !ProjectNodeEnum.UED_AUDIT.getCode().equals(projectNodeEnum.getCode()) && !ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getCode().equals(projectNodeEnum.getCode())) {
            throw new BaseBizRuntimeException("该节点无法发起评审");
        }
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectIdAndType(projectFlowDO.getProjectId(), projectNodeEnum.getCode());
        if (!CollectionUtils.isEmpty(projectFlowDos)) {
            projectFlowDos.sort(Comparator.comparing(ProjectFlowDO::getCreateDate).reversed());
            ProjectFlowDO oldFlowDo = projectFlowDos.get(0);
            if (FlowStatusEnum.AUDITING.getCode().equals(oldFlowDo.getStatus())) {
                throw new BaseBizRuntimeException(String.format("%s正在审核中,请不要重复发起", projectNodeEnum.getText()));
            }
            if (FlowStatusEnum.COMPLETE.getCode().equals(oldFlowDo.getStatus())) {
                throw new BaseBizRuntimeException(String.format("%s已通过,请不要重复发起", projectNodeEnum.getText()));
            }
            if (FlowStatusEnum.PRE_EDIT.getCode().equals(oldFlowDo.getStatus())) {
                projectFlowDO.setId(oldFlowDo.getId());
            }
        }
        ProjectNodeDO projectNodeDo = projectNodeMapper.getByName(projectFlowDO.getProjectId(), projectNodeEnum.getText());
        if (projectNodeDo == null) {
            throw new BaseBizRuntimeException(String.format("%s节点不存在", projectNodeEnum.getText()));
        }
        projectNodeMapper.updateActualDateById(projectNodeDo.getId(), null);
        //更新节点状态
        projectComponent.updateNodeStatus(projectFlowDO.getProjectId());

        projectNodeDo = projectNodeMapper.getByName(projectFlowDO.getProjectId(), ProjectNodeEnum.START_PLAN.getText());
        if (projectNodeDo == null) {
            //需求规划阶段被删除,详设评审为第一个节点,需要清空项目实际开始时间
            ProjectDO updateActualStartDateDO = new ProjectDO();
            updateActualStartDateDO.setId(projectFlowDO.getProjectId());
            updateActualStartDateDO.setActualStartDate(null);
            projectMapper.update(updateActualStartDateDO);
        }

        Integer newStatus = projectComponent.getStatus(projectFlowDO.getProjectId());
        if (!Objects.equal(oldProjectDO.getStatus(), newStatus)
                && !ProjectStatusEnum.INVALID.getCode().equals(oldProjectDO.getStatus())
                && !ProjectStatusEnum.SUSPEND.getCode().equals(oldProjectDO.getStatus())) {
            ProjectDO updateStatusDO = new ProjectDO();
            updateStatusDO.setId(projectFlowDO.getProjectId());
            updateStatusDO.setStatus(newStatus);
            projectMapper.update(updateStatusDO);
            // 日志处理
            projectLogComponent.addLogWhenStatusChange(oldProjectDO.getStatus(), newStatus, oldProjectDO.getId(), String.format("发起%s", projectNodeEnum.getText()));
        }

        String processInstanceId = startWorkflow(projectFlowAddReq);
        projectFlowDO.setFlowId(processInstanceId);
        projectFlowDO.setStatus(FlowStatusEnum.AUDITING.getCode());
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        projectFlowDO.setDocModifyDate(new Date());
        projectFlowDO.setDocModifyManId(userInfo.getId());
        projectFlowDO.setDocModifyMan(userInfo.getFullAlias());
        if (projectFlowDO.getId() == null) {
            projectFlowMapper.insert(projectFlowDO);
        } else {
            projectFlowMapper.update(projectFlowDO);
        }
        return BaseResult.success(processInstanceId);
    }

    @Override
    public BaseResult<Boolean> modifyDoc(ProjectFlowDocModifyReq projectFlowDocModifyReq) {
        List<ProjectFlowDO> flows = projectFlowMapper.getByProjectIdAndType(projectFlowDocModifyReq.getProjectId(),
                projectFlowDocModifyReq.getFlowType());
        if (flows.isEmpty()) {
            // 未生成过，生成一份项目流程
            ProjectFlowDO preEditFlow = new ProjectFlowDO();
            preEditFlow.setFlowId(StringUtils.EMPTY);
            preEditFlow.setFlowType(projectFlowDocModifyReq.getFlowType());
            preEditFlow.setProposerId(StringUtils.EMPTY);
            preEditFlow.setProposer(StringUtils.EMPTY);
            preEditFlow.setProjectId(projectFlowDocModifyReq.getProjectId());
            preEditFlow.setStatus(FlowStatusEnum.PRE_EDIT.getCode());
            preEditFlow.setReviewUrl(projectFlowDocModifyReq.getReviewUrl());
            projectFlowMapper.insert(preEditFlow);
            flows.add(preEditFlow);
        }
        ProjectFlowDO projectFlow = flows.get(0);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        projectFlow.setDocModifyManId(userInfo.getId());
        projectFlow.setDocModifyMan(userInfo.getFullAlias());
        projectFlow.setDocModifyDate(new Date());
        projectFlow.setReviewUrl(projectFlowDocModifyReq.getReviewUrl());
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
        if (reviewList != null) {
            for (int i = 0; i < reviewList.size(); i++) {
                PersonVO personVO = new PersonVO();
                personVO.setUserName(reviewList.get(i));
                personVO.setUserId(reviewIdList.get(i));
                reviews.add(personVO);
            }
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
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectIdAndType(oldFlowDo.getProjectId(),oldFlowDo.getFlowType());
        long count = projectFlowDos.stream().filter(a -> FlowStatusEnum.REJECT.getCode().equals(a.getStatus())).count();
        projectFlowDetailVO.setReturnCount(count);
        return BaseResult.success(projectFlowDetailVO);
    }

    @Override
    public BaseResult<List<ProjectFlowNodeVO>> getFlowNodeInfo(Long projectId) {
        List<ProjectFlowNodeVO> projectFlowNodeVos = new ArrayList<>();
        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectId(projectId);
        if (CollectionUtils.isEmpty(projectFlowDos)) {
            return BaseResult.success(projectFlowNodeVos);
        }
        // 分组,排序
        Map<Integer, List<ProjectFlowDO>> projectFlowMap = projectFlowDos.stream().sorted(Comparator.comparing(ProjectFlowDO::getCreateDate).reversed()).collect(Collectors.groupingBy(ProjectFlowDO::getFlowType, LinkedHashMap::new, Collectors.toList()));

        List<ProjectNodeDO> projectNodeDos = projectNodeMapper.get(projectId);
        projectNodeDos.forEach(p->{
         Integer projectNodeCode =  ProjectNodeEnum.getCodeByName(p.getName());
         List<ProjectFlowDO> projectFlowDOList =  projectFlowMap.get(projectNodeCode);
         if(CollectionUtils.isNotEmpty(projectFlowDOList)){
             ProjectFlowNodeVO projectFlowNodeVO = new ProjectFlowNodeVO();
             ProjectFlowDO projectFlowDO = projectFlowDOList.get(0);
             projectFlowNodeVO.setProjectFlowId(projectFlowDO.getId());
             projectFlowNodeVO.setProjectFlowStatus(projectFlowDO.getStatus());
             projectFlowNodeVO.setProjectNodeId(p.getId());
             projectFlowNodeVos.add(projectFlowNodeVO);
         }
        });
        return  BaseResult.success(projectFlowNodeVos);
    }

    @Override
    public BaseResult<Void> flushFlowEndDate() {
//        PageUtil.page(this::flushFlow);
//        List<ProjectFlowDO> projectFlowDOList = flushFlow(new QueryBase());

        return BaseResult.success();
    }


    private String startWorkflow(ProjectFlowAddReq projectFlowAddReq) {
        String processDefinitionKey = MessageTagEnum.NODE_MESSAGE_TAG_MAP.get(projectFlowAddReq.getFlowType());
        if (StringUtils.isEmpty(processDefinitionKey)) {
            throw new BaseBizRuntimeException("找不到对应的审批流程");
        }
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
        variables.put("createDate", DateUtils.getNewFormatDateString(DateUtils.now()));
        String baseUrl = domainName + "projectManagement/edit?id=%d&type=check";
        variables.put("projectUrl", String.format(baseUrl, projectFlowAddReq.getProjectId()));
        List<String> reviewIds = projectFlowAddReq.getReviews().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        variables.put("review", reviewIds);

        start.setApplicationName("forward");
        start.setProcessDefinitionKey(processDefinitionKey);
        start.setStartAccountId(projectFlowAddReq.getProposer().getUserId());
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        return epeiusClient.start(start);
    }
}
