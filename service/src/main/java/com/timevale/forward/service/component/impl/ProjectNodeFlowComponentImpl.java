package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeFlowMapper;
import com.timevale.forward.dal.dao.ProjectNodeRecordMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.ProjectNodeFlowDO;
import com.timevale.forward.dal.entity.ProjectNodeRecordDO;
import com.timevale.forward.model.enums.FlowStageEnum;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.lowcode.support.response.task.TaskHandleUserResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProjectNodeFlowComponentImpl implements ProjectNodeFlowComponent {

    @Resource
    private ProjectNodeFlowMapper projectNodeFlowMapper;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Resource
    private EpeiusClient epeiusClient;

    @Resource
    private ProjectNodeRecordMapper projectNodeRecordMapper;

    @Resource
    private ProjectNodeComponent projectNodeComponent;

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private ProjectMapper projectMapper;


    @Override
    public void process(ProjectNodeFlowDO projectNodeFlowDO, List<ProjectNodeDO> projectNodes) {
        log.info("节点审批流程发起,参数:{}", projectNodeFlowDO);
        if (projectNodeFlowDO == null) {
            return;
        }
        List<ProjectNodeFlowDO> projectNodeFlows = projectNodeFlowMapper.getByProjectId(projectNodeFlowDO.getProjectId());
        boolean match = projectNodeFlows.stream().anyMatch(a -> com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode().equals(a.getStatus()));
        if (match) {
            return;
        }
        Date oldPlanEndDate = DateUtil.getEndOfDay(projectNodeFlowDO.getPublishDate());
        Date planEndDate = DateUtil.getEndOfDay(projectNodeFlowDO.getChangePublishDate());
        if (oldPlanEndDate.before(planEndDate)) {
            Long seconds = elapsedTimeClient.getElapsedTime(oldPlanEndDate, planEndDate);
            BigDecimal elapsedTime = new BigDecimal(seconds.toString());
            elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
            if (BigDecimal.ZERO.equals(elapsedTime)) {
                return;
            }
            projectNodeFlowDO.setDelayDay(elapsedTime);
//            projectNodeFlowDO.setDelayDay(BigDecimal.valueOf(1));
            projectNodeFlowDO.setStage(FlowStageEnum.FIRST.getCode());
            projectNodeFlowDO.setFlowId(startFlow(projectNodeFlowDO, projectNodes));
            projectNodeFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode());
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
            projectNodeFlowDO.setCreateMan(operator);
            projectNodeFlowDO.setCreateManId(userInfo.getId());
            projectNodeFlowMapper.insert(projectNodeFlowDO);
        }
    }

    @Override
    public void updateProjectNodeInfo(String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            log.info("流程id为空");
            return;
        }
        ProcessResponse processInfo = epeiusClient.getProcessInfo(processInstanceId);
        List<String> currentTaskIdList = processInfo.getCurrentTaskIdList();
        if (CollectionUtils.isEmpty(currentTaskIdList)) {
            log.info("任务id为空");
            return;
        }
        String processStatus = processInfo.getProcessStatus();
        log.info("返回流程信息 processInfo={}", processInfo);
        ProjectNodeFlowDO projectNodeFlowDO = projectNodeFlowMapper.get(null, processInstanceId);
        if (projectNodeFlowDO == null) {
            log.info("无发布正式节点审批流程 flowId={}", processInstanceId);
            return;
        }
        Map<String, Object> flowData = processInfo.getFlowData();
        if (FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            projectNodeFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.REJECT.getCode());
            String rejectReason = flowData.get("rejectReason") == null ? StringUtils.EMPTY : String.valueOf(flowData.get("rejectReason"));
            projectNodeFlowDO.setReviewFailReason(rejectReason);
        } else if (FlowStatusEnum.WITHDRAW.getValue().equals(processStatus)) {
            projectNodeFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.WITHDRAW.getCode());
        } else if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            projectNodeFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.COMPLETE.getCode());

        }
        List<String> reviewList = new ArrayList<>();
        List<String> reviewIdList = new ArrayList<>();
        if (FlowStageEnum.FIRST.getCode().equals(projectNodeFlowDO.getStage())) {
            if (StringUtils.isNotEmpty(projectNodeFlowDO.getPd())) {
                reviewList.add(projectNodeFlowDO.getPd());
                reviewIdList.add(projectNodeFlowDO.getPdId());
            }
            if (StringUtils.isNotEmpty(projectNodeFlowDO.getBiz())) {
                List<String> biz = JSONObject.parseArray(projectNodeFlowDO.getBiz(), String.class);
                List<String> bizId = JSONObject.parseArray(projectNodeFlowDO.getBizId(), String.class);
                reviewList.addAll(biz);
                reviewIdList.addAll(bizId);
            }
        } else {
            if (StringUtils.isNotEmpty(projectNodeFlowDO.getPo())) {
                reviewList.add(projectNodeFlowDO.getPo());
                reviewIdList.add(projectNodeFlowDO.getPoId());
            } else {
                reviewList.add(projectNodeFlowDO.getD());
                reviewIdList.add(projectNodeFlowDO.getDid());
            }
        }
        Map<String, String> reviewMap = new HashMap<>();
        for (int i = 0; i < reviewList.size(); i++) {
            reviewMap.put(reviewIdList.get(i), reviewList.get(i));
        }

        TaskHandleUserResponse taskHandleUserList = epeiusClient.getTaskHandleUserList(currentTaskIdList.get(0));
        log.info("返回人员信息 taskHandleUserList={}", taskHandleUserList);
        List<String> passIds = taskHandleUserList.getPassedUserList().stream().map(TaskHandleUserResponse.TaskUser::getAccountId).collect(Collectors.toList());
        List<String> passAlias = new ArrayList<>();
        passIds.forEach(a -> {
            passAlias.add(reviewMap.get(a));
        });

        List<String> rejectIds = taskHandleUserList.getRejectUserList().stream().map(TaskHandleUserResponse.TaskUser::getAccountId).collect(Collectors.toList());
        List<String> rejectAlias = new ArrayList<>();
        rejectIds.forEach(a -> {
            rejectAlias.add(reviewMap.get(a));
        });

        reviewIdList.removeAll(passIds);
        reviewIdList.removeAll(rejectIds);

        List<String> unReviewAlias = new ArrayList<>();
        reviewIdList.forEach(a -> {
            unReviewAlias.add(reviewMap.get(a));
        });
        projectNodeFlowDO.setReviewFailId(CollectionUtils.isEmpty(rejectIds) ? StringUtils.EMPTY : JSONObject.toJSONString(rejectIds));
        projectNodeFlowDO.setReviewFail(CollectionUtils.isEmpty(rejectAlias) ? StringUtils.EMPTY : JSONObject.toJSONString(rejectAlias));
        projectNodeFlowDO.setUnreviewedId(CollectionUtils.isEmpty(reviewIdList) ? StringUtils.EMPTY : JSONObject.toJSONString(reviewIdList));
        projectNodeFlowDO.setUnreviewed(CollectionUtils.isEmpty(unReviewAlias) ? StringUtils.EMPTY : JSONObject.toJSONString(unReviewAlias));

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();
        projectNodeFlowDO.setModifyMan(operator);
        projectNodeFlowDO.setModifyManId(userInfo.getId());

        log.info("更新的数据 projectNodeFlowDO={}", projectNodeFlowDO);
        projectNodeFlowMapper.update(projectNodeFlowDO);

        List<ProjectNodeDO> projectNodes = JSONObject.parseArray(String.valueOf(flowData.get("projectNodes")), ProjectNodeDO.class);
        if (FlowStageEnum.FIRST.getCode().equals(projectNodeFlowDO.getStage()) && FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            projectNodeFlowDO.setStage(FlowStageEnum.SECOND.getCode());
            projectNodeFlowDO.setLastFlowId(projectNodeFlowDO.getFlowId());
            projectNodeFlowDO.setFlowId(startFlow(projectNodeFlowDO, projectNodes));
            projectNodeFlowDO.setStatus(com.timevale.forward.model.enums.FlowStatusEnum.AUDITING.getCode());
            projectNodeFlowDO.setReviewFailReason(StringUtils.EMPTY);
            projectNodeFlowDO.setReviewFail(StringUtils.EMPTY);
            projectNodeFlowDO.setReviewFailId(StringUtils.EMPTY);
            String unreviewed = StringUtils.isEmpty(projectNodeFlowDO.getD()) ? projectNodeFlowDO.getPo() : projectNodeFlowDO.getD();
            String unreviewedId = StringUtils.isEmpty(projectNodeFlowDO.getDid()) ? projectNodeFlowDO.getPoId() : projectNodeFlowDO.getDid();
            projectNodeFlowDO.setUnreviewed(unreviewed);
            projectNodeFlowDO.setUnreviewedId(unreviewedId);
            projectNodeFlowDO.setCreateMan(operator);
            projectNodeFlowDO.setCreateManId(userInfo.getId());
            projectNodeFlowMapper.insert(projectNodeFlowDO);
        }
        if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            Long projectId = projectNodeFlowDO.getProjectId();
            projectNodeComponent.add(projectNodes, projectId);

            projectComponent.updateNodeStatus(projectId);

            ProjectDO projectDO = projectMapper.get(projectId);
            projectComponent.fillInfo(projectNodes, projectDO);
            projectMapper.update(projectDO);

            insertProjectNodeRecord(projectNodeFlowDO.getProjectId(), projectNodes);
        }
    }

    @Override
    public void insertProjectNodeRecord(Long projectId, List<ProjectNodeDO> projectNodes) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String operator = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();

        BigDecimal max = projectNodeRecordMapper.list(projectId).stream().map(ProjectNodeRecordDO::getVersion)
                .max(Comparator.comparing(BigDecimal::abs)).orElse(BigDecimal.ZERO);

        List<ProjectNodeRecordDO> recordDOList = projectNodes.stream().map(a -> {
            ProjectNodeRecordDO p = new ProjectNodeRecordDO();
            p.setProjectId(projectId);
            p.setName(a.getName());
            p.setPlanDate(a.getPlanDate());
            p.setVersion(max.add(BigDecimal.valueOf(1)));
            p.setCreateMan(operator);
            p.setCreateManId(userInfo.getId());
            return p;
        }).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(recordDOList)) {
            projectNodeRecordMapper.batchInsert(recordDOList);
        }
    }


    private String startFlow(ProjectNodeFlowDO projectNodeFlowDO, List<ProjectNodeDO> projectNodes) {
        List<ProjectNodeFlowDO> projectNodeFlows = projectNodeFlowMapper.getByProjectId(projectNodeFlowDO.getProjectId());
        long count = projectNodeFlows.stream().filter(a -> com.timevale.forward.model.enums.FlowStatusEnum.COMPLETE.getCode().equals(a.getStatus())).count();
        StartProcessRequest start = new StartProcessRequest();
        Map<String, Object> variables = new HashMap<>();
        variables.put("projectNodes", JSONObject.toJSONString(projectNodes));
        variables.put("reason", projectNodeFlowDO.getReason());
        variables.put("pd", projectNodeFlowDO.getPd());
        List<String> biz = JSONObject.parseArray(projectNodeFlowDO.getBiz(), String.class);
        variables.put("biz", StringUtils.join(biz, ","));
        variables.put("po", projectNodeFlowDO.getPo());
        variables.put("d", projectNodeFlowDO.getD());
        variables.put("publishDate", DateUtil.parseToString(projectNodeFlowDO.getPublishDate(), DateFormatConst.DATE_FORMAT));
        variables.put("changePublishDate", DateUtil.parseToString(projectNodeFlowDO.getChangePublishDate(), DateFormatConst.DATE_FORMAT));
        variables.put("delayDay", projectNodeFlowDO.getDelayDay());
        variables.put("changeCount", count);
        List<String> reviewIds = new ArrayList<>();
        if (FlowStageEnum.FIRST.getCode().equals(projectNodeFlowDO.getStage())) {
            reviewIds.add(projectNodeFlowDO.getPdId());
            reviewIds.addAll(JSONObject.parseArray(projectNodeFlowDO.getBizId(), String.class));
        } else if (!StringUtils.isEmpty(projectNodeFlowDO.getPoId())) {
            reviewIds.add(projectNodeFlowDO.getPoId());
        } else {
            reviewIds.add(projectNodeFlowDO.getDid());
        }
        String lastFlowId = projectNodeFlowDO.getLastFlowId();
        if (!StringUtils.isEmpty(lastFlowId)) {
            ProjectNodeFlowDO lastFlow = projectNodeFlowMapper.get(null, lastFlowId);
            List<String> reviewFail = JSONObject.parseArray(projectNodeFlowDO.getReviewFail(), String.class);
            variables.put("reviewFail", StringUtils.join(reviewFail, ","));
            variables.put("reviewFailReason", lastFlow.getReviewFailReason());
        }
        variables.put("reviews", reviewIds);
        if (StringUtils.isEmpty(projectNodeFlowDO.getCreateMan())) {
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            variables.put("proposer", userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            start.setStartAccountId(userInfo.getId());
        } else {
            variables.put("proposer", projectNodeFlowDO.getCreateMan());
            start.setStartAccountId(projectNodeFlowDO.getCreateManId());
        }
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_publishOfficeReview");
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        return epeiusClient.start(start);
    }
}
