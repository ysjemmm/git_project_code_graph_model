package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeFlowMapper;
import com.timevale.forward.dal.dao.ProjectNodeRecordMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.component.ProjectNodeFlowComponent;
import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.observer.event.WorkflowRejectMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.lowcode.support.response.task.TaskHandleUserResponse;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.query.QueryBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
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
    @Resource
    private BizChangeLogMapper bizChangeLogMapper;
    @Resource
    private MessageEventPublisher messageEventPublisher;
    @Resource
    private CommonConfig config;

    @Override
    public void process(ProjectNodeFlowDO projectNodeFlowDO, List<ProjectNodeDO> projectNodes) {
        log.info("节点审批流程发起,参数:{}", projectNodeFlowDO);
        if (projectNodeFlowDO == null) {
            throw new BaseBizRuntimeException("请填写流程表单数据后重新发起");
        }
        List<ProjectNodeFlowDO> projectNodeFlows = projectNodeFlowMapper.getByProjectId(projectNodeFlowDO.getProjectId());
        boolean match = projectNodeFlows.stream().anyMatch(a -> ForwardFlowStatusEnum.AUDITING.getCode().equals(a.getStatus()));
        if (match) {
            throw new BaseBizRuntimeException("存在正在审核中的审批流程,请撤销后重新发起");
        }

        Date notNull = projectNodeFlowDO.getPjEstablishPublishDate() != null ? projectNodeFlowDO.getPjEstablishPublishDate() : projectNodeFlowDO.getPublishDate();
        Date oldPlanEndDate = DateUtil.getEndOfDay(notNull);
        Date changePublishDate = projectNodeFlowDO.getChangePublishDate();
        if (changePublishDate == null) {
            throw new BaseBizRuntimeException("变更发布日期不能为空");
        }
        Date planEndDate = DateUtil.getEndOfDay(changePublishDate);

        if (oldPlanEndDate.before(planEndDate)) {
            Long seconds = elapsedTimeClient.getElapsedTime(oldPlanEndDate, planEndDate);
            BigDecimal elapsedTime = new BigDecimal(seconds.toString());
            elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
            projectNodeFlowDO.setDelayDay(elapsedTime);
            UserInfo userInfo = LocalSessionUtils.getUserInfo();
            projectNodeFlowDO.setStatus(ForwardFlowStatusEnum.AUDITING.getCode());
            projectNodeFlowDO.setCreateMan(userInfo.getFullAlias());
            projectNodeFlowDO.setCreateManId(userInfo.getId());

            Integer stage = StrUtil.isEmpty(projectNodeFlowDO.getBizId()) && StrUtil.isEmpty(projectNodeFlowDO.getPdId()) ? FlowStageEnum.SECOND.getCode() : FlowStageEnum.FIRST.getCode();
            projectNodeFlowDO.setStage(stage);

            // 发起流程
            String flowId = startFlow(projectNodeFlowDO, projectNodes);
            projectNodeFlowDO.setFlowId(flowId);

            projectNodeFlowMapper.insert(projectNodeFlowDO);
        }
    }

    @Override
    public void updateProjectNodeInfo(String processInstanceId) {
        if (StringUtils.isEmpty(processInstanceId)) {
            log.info("流程id为空");
            return;
        }

        // 获取流程信息
        ProcessResponse processInfo = epeiusClient.getProcessInfo(processInstanceId);
        List<String> currentTaskIdList = processInfo.getCurrentTaskIdList();
        if (CollectionUtils.isEmpty(currentTaskIdList)) {
            log.info("任务id为空");
            return;
        }

        // 获取流程状态
        String processStatus = processInfo.getProcessStatus();
        log.info("返回流程信息 processInfo={}", processInfo);

        // 查询产研流程数据
        ProjectNodeFlowDO projectNodeFlowDO = projectNodeFlowMapper.get(null, processInstanceId);
        if (projectNodeFlowDO == null) {
            log.info("无发布正式节点审批流程 flowId={}", processInstanceId);
            return;
        }

        // 获取对应项目
        Long projectId = projectNodeFlowDO.getProjectId();
        ProjectDO projectDO = projectMapper.get(projectId);

        Map<String, Object> flowData = processInfo.getFlowData();
        if (FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            projectNodeFlowDO.setStatus(ForwardFlowStatusEnum.REJECT.getCode());
            String rejectReason = flowData.get("rejectReason") == null ? "" : String.valueOf(flowData.get("rejectReason"));
            projectNodeFlowDO.setReviewFailReason(rejectReason);
            projectNodeFlowDO.setFlowEndDate(new Date());
        } else if (FlowStatusEnum.WITHDRAW.getValue().equals(processStatus)) {
            projectNodeFlowDO.setStatus(ForwardFlowStatusEnum.WITHDRAW.getCode());
            projectNodeFlowDO.setFlowEndDate(new Date());
        } else if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            projectNodeFlowDO.setStatus(ForwardFlowStatusEnum.COMPLETE.getCode());
            projectNodeFlowDO.setFlowEndDate(new Date());
        }

        //1审核人员处理
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
            if (StrUtil.isNotBlank(projectNodeFlowDO.getD())) {
                reviewList.add(projectNodeFlowDO.getD());
                reviewIdList.add(projectNodeFlowDO.getDid());
            } else {
                reviewList.add(projectDO.getSr());
                reviewIdList.add(projectDO.getSrId());
            }
        }

        Map<String, String> reviewMap = new HashMap<>();
        for (int i = 0; i < reviewList.size(); i++) {
            reviewMap.put(reviewIdList.get(i), reviewList.get(i));
        }

        TaskHandleUserResponse taskHandleUserList = epeiusClient.getTaskHandleUserList(currentTaskIdList.get(0));
        log.info("返回人员信息 taskHandleUserList={}", taskHandleUserList);
        List<String> passIds = taskHandleUserList.getPassedUserList().stream().map(TaskHandleUserResponse.TaskUser::getAccountId).collect(Collectors.toList());

        // 评审不通过人员
        List<String> rejectIds = taskHandleUserList.getRejectUserList().stream().map(TaskHandleUserResponse.TaskUser::getAccountId).collect(Collectors.toList());
        List<String> rejectAlias = rejectIds.stream().map(reviewMap::get).collect(Collectors.toList());
        reviewIdList.removeAll(passIds);
        reviewIdList.removeAll(rejectIds);

        // 未评审人员
        List<String> unReviewAlias = reviewIdList.stream().map(reviewMap::get).collect(Collectors.toList());

        projectNodeFlowDO.setReviewFailId(CollectionUtils.isEmpty(rejectIds) ? "" : JSONObject.toJSONString(rejectIds));
        projectNodeFlowDO.setReviewFail(CollectionUtils.isEmpty(rejectAlias) ? "" : JSONObject.toJSONString(rejectAlias));
        projectNodeFlowDO.setUnreviewedId(CollectionUtils.isEmpty(reviewIdList) ? "" : JSONObject.toJSONString(reviewIdList));
        projectNodeFlowDO.setUnreviewed(CollectionUtils.isEmpty(unReviewAlias) ? "" : JSONObject.toJSONString(unReviewAlias));

        projectNodeFlowDO.setModifyMan(projectNodeFlowDO.getCreateMan());
        projectNodeFlowDO.setModifyManId(projectNodeFlowDO.getCreateManId());

        log.info("更新的数据 projectNodeFlowDO={}", projectNodeFlowDO);
        projectNodeFlowMapper.update(projectNodeFlowDO);

        List<ProjectNodeDO> projectNodes = JSONObject.parseArray(String.valueOf(flowData.get("projectNodes")), ProjectNodeDO.class);

        // 如果流程审批被拒绝
        if (FlowStatusEnum.REJECT.getValue().equals(processStatus)) {
            if (FlowStageEnum.FIRST.getCode().equals(projectNodeFlowDO.getStage())) {
                // 如果为一阶段，再发出一条流程
                projectNodeFlowDO.setStage(FlowStageEnum.SECOND.getCode());
                projectNodeFlowDO.setLastFlowId(projectNodeFlowDO.getFlowId());
                String flowId = startFlow(projectNodeFlowDO, projectNodes);

                // 记录
                projectNodeFlowDO.setFlowId(flowId);
                projectNodeFlowDO.setStatus(ForwardFlowStatusEnum.AUDITING.getCode());
                projectNodeFlowDO.setReviewFailReason(StringUtils.EMPTY);
                projectNodeFlowDO.setReviewFail(StringUtils.EMPTY);
                projectNodeFlowDO.setReviewFailId(StringUtils.EMPTY);
                String unreviewed = StringUtils.isEmpty(projectNodeFlowDO.getD()) ? projectNodeFlowDO.getPo() : projectNodeFlowDO.getD();
                String unreviewedId = StringUtils.isEmpty(projectNodeFlowDO.getDid()) ? projectNodeFlowDO.getPoId() : projectNodeFlowDO.getDid();
                projectNodeFlowDO.setUnreviewed(JSONObject.toJSONString(Lists.newArrayList(unreviewed)));
                projectNodeFlowDO.setUnreviewedId(JSONObject.toJSONString(Lists.newArrayList(unreviewedId)));
                projectNodeFlowMapper.insert(projectNodeFlowDO);
            } else {
                // 如果为二阶段则不再发送流程
                messageEventPublisher.publish(new WorkflowRejectMsgEvent(
                        this,
                        "项目计划变更审批流程",
                        projectNodeFlowDO.getCreateManId(),
                        currentTaskIdList.get(0),
                        projectNodeFlowDO.getCreateMan(),
                        DateUtil.parseToString(projectNodeFlowDO.getCreateDate(), DateUtil.DEFAULT_DATE_FORMAT),
                        projectNodeFlowDO.getReviewFailReason()
                ));
            }
        }
        if (FlowStatusEnum.FLOW_COMPLETE.getValue().equals(processStatus)) {
            //3.流程通过后,更新信息

            projectNodeComponent.updateNodePlanDate(projectNodes, projectId);

            projectComponent.updateNodeStatus(projectId);


            Date oldEndDate = projectDO.getPlanEndDate();

            projectNodes = projectNodeComponent.sort(projectNodes);
            ProjectNodeDO first = projectNodes.get(0);
            ProjectNodeDO last = projectNodes.get(projectNodes.size() - 1);
            projectDO.setPlanStartDate(first.getPlanDate());
            projectDO.setPlanEndDate(last.getPlanDate());
            projectMapper.fullUpdateById(projectDO);

            insertProjectNodeRecord(projectNodeFlowDO.getProjectId(), projectNodes, projectNodeFlowDO);

            // 创建变更记录
            String oldValue = DateUtil.parseToString(oldEndDate, DateFormatConst.DATE_FORMAT);
            String newValue = DateUtil.parseToString(projectDO.getPlanEndDate(), DateFormatConst.DATE_FORMAT);
            if (!Objects.equals(oldValue, newValue)) {
                BizChangeLogDO bizChangeLogDO = new BizChangeLogDO()
                        .setMainId(projectId)
                        .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                        .setField(BizChangeLogFieldEnum.PLAN_END_DATE.getText())
                        .setOldValue(oldValue)
                        .setNewValue(newValue);
                bizChangeLogDO.setCreateMan(projectNodeFlowDO.getCreateMan());
                bizChangeLogDO.setCreateManId(projectNodeFlowDO.getCreateManId());
                bizChangeLogDO.setContent(String.format("{\"taskId\": \"%s\"}", currentTaskIdList.get(0)));
                bizChangeLogMapper.insert(bizChangeLogDO);
            }
        }
    }

    @Override
    public void insertProjectNodeRecord(Long projectId, List<ProjectNodeDO> projectNodes) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        ProjectNodeFlowDO projectNodeFlowDO = new ProjectNodeFlowDO();
        projectNodeFlowDO.setCreateManId(userInfo.getId());
        projectNodeFlowDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        insertProjectNodeRecord(projectId, projectNodes, projectNodeFlowDO);
    }

    @Override
    public List<ProjectNodeFlowDO> flushCompleteFlow(QueryBase queryBase) {
        PageHelper.startPage(queryBase.getPageNum(), queryBase.getPageSize());

        List<ProjectNodeFlowDO> projectNodeFlowDOList = projectNodeFlowMapper.pageCompleteFlow();

        PageInfo<ProjectNodeFlowDO> pageInfo = new PageInfo<>(projectNodeFlowDOList);

        if (CollectionUtils.isEmpty(projectNodeFlowDOList)) {
            return projectNodeFlowDOList;
        }

        for (ProjectNodeFlowDO projectNodeFlowDO : projectNodeFlowDOList) {
            Date endTime;

            try {
                ProcessResponse processResponse = epeiusClient.getProcessInfo(projectNodeFlowDO.getFlowId());
                endTime = processResponse.getEndTime();
            } catch (Exception e) {
                //使用更新时间更新
                endTime = projectNodeFlowDO.getModifyDate();
            }

            projectNodeFlowDO.setFlowId(null);
            projectNodeFlowDO.setModifyDate(null);
            projectNodeFlowDO.setFlowEndDate(endTime);
        }

        projectNodeFlowMapper.batchUpdateFlowEndTime(projectNodeFlowDOList);

        return pageInfo.getList();
    }

    private void insertProjectNodeRecord(Long projectId, List<ProjectNodeDO> projectNodes, ProjectNodeFlowDO projectNodeFlowDO) {
        BigDecimal max = projectNodeRecordMapper.list(projectId).stream()
                .map(ProjectNodeRecordDO::getVersion)
                .max(Comparator.comparing(BigDecimal::abs))
                .orElse(BigDecimal.ZERO);

        List<ProjectNodeRecordDO> recordDOList = projectNodes.stream()
                .map(a -> {
                    ProjectNodeRecordDO p = new ProjectNodeRecordDO();
                    p.setName(a.getName());
                    p.setProjectId(projectId);
                    p.setPlanDate(a.getPlanDate());
                    p.setVersion(max.add(BigDecimal.ONE));
                    p.setCreateMan(projectNodeFlowDO.getCreateMan());
                    p.setCreateManId(projectNodeFlowDO.getCreateManId());
                    return p;
                }).collect(Collectors.toList());

        if (CollUtil.isNotEmpty(recordDOList)) {
            projectNodeRecordMapper.batchInsert(recordDOList);
        }
    }

    private String startFlow(ProjectNodeFlowDO projectNodeFlowDO, List<ProjectNodeDO> projectNodes) {
        List<ProjectNodeFlowDO> projectNodeFlows = projectNodeFlowMapper.getByProjectId(projectNodeFlowDO.getProjectId());
        long count = projectNodeFlows.stream().filter(a -> ForwardFlowStatusEnum.COMPLETE.getCode().equals(a.getStatus())).count();

        // 查询对应项目
        ProjectDO projectDO = projectMapper.get(projectNodeFlowDO.getProjectId());

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
        variables.put("pjEstablishPublishDate", DateUtil.parseToString(projectNodeFlowDO.getPjEstablishPublishDate(), DateFormatConst.DATE_FORMAT));
        variables.put("delayDay", projectNodeFlowDO.getDelayDay());
        variables.put("changeCount", count);
        variables.put("changeType", ChangeTypeEnum.getTextByCode(projectNodeFlowDO.getChangeType()));
        variables.put("otherReason", projectNodeFlowDO.getOtherReason());
        variables.put("projectName", projectNodeFlowDO.getProjectName());
        variables.put("detailLink", String.format(config.getCommonViewUrl(),TabEnum.PROJECT_MANAGEMENT.getText(),projectNodeFlowDO.getProjectId()));

        List<String> reviewIds = new ArrayList<>();
        List<String> reviews = new ArrayList<>();

        // 第一阶段审批人为产品经理和业务方或SR，第二阶段为pbu负责人或SR
        if (FlowStageEnum.FIRST.getCode().equals(projectNodeFlowDO.getStage())) {
            Integer kind = projectDO.getKind();
            if (ProjectKindEnum.PBG_OTN.getCode().equals(kind)) {
                reviews.add(projectDO.getSr());
                reviewIds.add(projectDO.getSrId());
            } else {
                List<String> bizId = JSONObject.parseArray(projectNodeFlowDO.getBizId(), String.class);
                if (CollectionUtils.isNotEmpty(bizId)) {
                    reviewIds.addAll(bizId);
                    reviews.addAll(JSONObject.parseArray(projectNodeFlowDO.getBiz(), String.class));
                }
                if (StringUtils.isNotEmpty(projectNodeFlowDO.getPdId())) {
                    reviewIds.add(projectNodeFlowDO.getPdId());
                    reviews.add(projectNodeFlowDO.getPd());
                }
            }
        } else if (StrUtil.isNotBlank(projectNodeFlowDO.getD())) {
            reviews.add(projectNodeFlowDO.getD());
            reviewIds.add(projectNodeFlowDO.getDid());
        } else {
            reviews.add(projectDO.getSr());
            reviewIds.add(projectDO.getSrId());
        }

        String lastFlowId = projectNodeFlowDO.getLastFlowId();
        if (StrUtil.isNotBlank(lastFlowId)) {
            ProjectNodeFlowDO lastFlow = projectNodeFlowMapper.get(null, lastFlowId);
            List<String> reviewFail = JSONObject.parseArray(projectNodeFlowDO.getReviewFail(), String.class);
            variables.put("reviewFail", StringUtils.join(reviewFail, ","));
            variables.put("reviewFailReason", lastFlow.getReviewFailReason());
        }

        //流程发起,未审核人员=评审人员
        projectNodeFlowDO.setUnreviewedId(JSONObject.toJSONString(reviewIds));
        projectNodeFlowDO.setUnreviewed(JSONObject.toJSONString(reviews));
        projectNodeFlowDO.setFlowType(ProjectNodeEnum.PUBLISH_OFFICIAL.getCode());
        variables.put("reviews", reviewIds);
        variables.put("proposer", projectNodeFlowDO.getCreateMan());

        // 项目表单信息, 直接覆盖，鬼才来重构
        variables.put("kind", ProjectKindEnum.getTextByCode(projectDO.getKind()));
        variables.put("type", ProjectTypeEnum.getTextByCode(projectDO.getType()));
        variables.put("level", ProjectLevelEnum.getTextByCode(projectDO.getLevel()));
        variables.put("sr", projectDO.getSr());
        variables.put("srId", projectDO.getSrId());

        start.setStartAccountId(projectNodeFlowDO.getCreateManId());
        start.setApplicationName("forward");
        start.setProcessDefinitionKey("forward_publishOfficeReview");
        start.setVariables(variables);
        start.setEpeVirtualProcessSwitch(false);
        return epeiusClient.start(start);
    }
}
