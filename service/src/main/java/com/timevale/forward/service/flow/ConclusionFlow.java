package com.timevale.forward.service.flow;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectEvaluateCopier;
import com.timevale.forward.service.flow.model.TargetStatusModel;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.epeius.model.ConclusionVar;
import com.timevale.forward.service.integration.epeius.model.ProjectEvaluateVar;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/03/16 14:01
 */
@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class ConclusionFlow {
    final private CommonConfig commonConfig;
    final private EpeiusClient epeiusClient;
    final private UserComponent userComponent;
    final private TaskComponent taskComponent;
    final private ProjectMapper projectMapper;
    final private PersonComponent personComponent;
    final private ProjectLogComponent logComponent;
    final private ProjectComponent projectComponent;
    final private ProjectFlowMapper projectFlowMapper;
    final private BizLabelComponent bizLabelComponent;
    final private ProjectEvaluateMapper evaluateMapper;
    final private EvaluateDimensionMapper dimensionMapper;
    final private InnerUserPersonClient innerUserPersonClient;
    final private ProductDemandComponent productDemandComponent;
    final private ProjectMemberEvaluateMapper memberEvaluateMapper;
    final private ProjectProductDemandComponent projectProductDemandComponent;

    /**
     * 结项工作流——发起
     *
     * @param projectId 项目id
     * @return {@link String}
     */
    @Transactional(rollbackFor = Exception.class)
    public String start(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 当前用户发起人id
        String startAccountId = LocalSessionUtils.getUserInfo().getId();

        // 项目地址
        String projectUrl = projectComponent.getUrl(projectId);

        // 项目pm，PMO
        String pmId = projectDO.getPmId();
        List<String> pmoIdList = userComponent.getAllPmo(commonConfig.getEvalPmoGroup());

        // 产品经理上级
        List<PersonDO> pds = personComponent.select(projectId, PersonTypeEnum.PROJECT_PD.getCode());
        Set<String> pdSuperiors = pds.parallelStream()
                .flatMap(e -> innerUserPersonClient.getAllSuperiorByAccount(e.getUserId(), false).stream())
                .collect(Collectors.toSet());
        String containPd = YesOrNoEnum.getTextByCode(CollUtil.isNotEmpty(pdSuperiors));

        // 发起人是否为项目负责人或者1-n产研负责人
        String principalId = projectDO.getPrincipalId();
        String otnPrincipalId = projectDO.getOtnPrincipalId();
        boolean startIsPrincipal = startAccountId.equals(principalId) || startAccountId.equals(otnPrincipalId);
        String isPrincipal = YesOrNoEnum.getTextByCode(startIsPrincipal);

        // 项目评价信息
        List<ProjectEvaluateDO> evaluateDOList = evaluateMapper.getByProjectId(projectId);
        List<EvaluateDimensionDO> dimensionDOList = dimensionMapper.getByKind(projectDO.getKind());
        ImmutableMap<Long, EvaluateDimensionDO> dimensionDOMap = Maps.uniqueIndex(dimensionDOList, EvaluateDimensionDO::getId);

        // 项目评价参数
        List<ProjectEvaluateVar> evaluates = evaluateDOList.stream()
                .map(e -> ProjectEvaluateCopier.INSTANCE.do2var(e, dimensionDOMap.get(e.getEvaluateDimensionId())))
                .collect(Collectors.toList());

        // 审批人评价参数
        List<ProjectEvaluateVar> reviewerEvaluates = evaluates.stream()
                .map(e -> new ProjectEvaluateVar()
                        .setScores(e.getScores())
                        .setScoresDesc(e.getScoresDesc())
                        .setDimensionId(e.getDimensionId())
                        .setDimensionName(e.getDimensionName())
                        .setScoresExplain(e.getScoresExplain()))
                .collect(Collectors.toList());

        // 计划总工作量
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);
        String planWorkloadSum = memberEvaluateDOList.stream()
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .toString();

        // 工作量计算积分
        String pointsWorkloadSum = memberEvaluateDOList.stream()
                .filter(ProjectMemberEvaluateDO::getIncludeStat)
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .toString();

        // 项目参数填装
        ConclusionVar conclusionVar = ProjectEvaluateCopier.INSTANCE.do2var(projectDO);

        conclusionVar.setPmId(pmId);
        conclusionVar.setPMOIds(pmoIdList);
        conclusionVar.setContainPd(containPd);
        conclusionVar.setIsPrincipal(isPrincipal);
        conclusionVar.setPrincipalId(principalId);
        conclusionVar.setPdSuperiorIds(pdSuperiors);
        conclusionVar.setOtnPrincipalId(otnPrincipalId);

        conclusionVar.setProjectUrl(projectUrl);
        conclusionVar.setEvaluates(evaluates);
        conclusionVar.setPlanWorkloadSum(planWorkloadSum);
        conclusionVar.setPointsWorkloadSum(pointsWorkloadSum);
        conclusionVar.setReviewerEvaluates(reviewerEvaluates);

        // 项目参数转换为Map
        Map<String, Object> variables = BeanUtil.beanToMap(conclusionVar);

        // 流程参数填装
        StartProcessRequest startProcessReq = new StartProcessRequest();
        startProcessReq.setVariables(variables);
        startProcessReq.setStartAccountId(startAccountId);
        startProcessReq.setEpeVirtualProcessSwitch(false);
        startProcessReq.setApplicationName(CommonConstant.APP);
        startProcessReq.setProcessDefinitionKey(MessageTagEnum.FORWARD_PROJECT_CONCLUSION.getText());

        return epeiusClient.start(startProcessReq);
    }

    /**
     * 结项工作流——审批完成回调处理
     *
     * @param processInstanceId 流程实例id
     */
    @Transactional(rollbackFor = Exception.class)
    public void complete(String processInstanceId) {
        AssertUtil.checkState(StrUtil.isNotBlank(processInstanceId), "流程id为空");

        // 获取项目流程信息
        ProjectFlowDO projectFlowDO = projectFlowMapper.getByFlowId(processInstanceId);
        AssertUtil.notNull(projectFlowDO, "流程不存在");

        // 判断项目流程状态
        AssertUtil.checkState(ForwardFlowStatusEnum.AUDITING.getCode().equals(projectFlowDO.getStatus()), "流程已结束");

        // 获取低代码流程信息
        ProcessResponse processInfo  = epeiusClient.getProcessInfo(processInstanceId);
        AssertUtil.notNull(processInfo, "流程信息为空");

        // 配置用户信息
        LocalSessionUtils.setUserInfo(projectFlowDO.getProposerId(), projectFlowDO.getProposer());

        // 查询对应项目
        final Long projectId = projectFlowDO.getProjectId();
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectFlowDO, "项目不存在");

        // 更新项目流程状态
        String processStatus = processInfo.getProcessStatus();
        Optional.ofNullable(ForwardFlowStatusEnum.getByValue(processStatus))
                .flatMap(e -> Optional.ofNullable(e.getCode()))
                .ifPresent(e -> projectFlowMapper.updateStatus(projectFlowDO.getId(), e));

        // 只有审批通过，需要更新项目评价信息
        if (ObjectUtil.notEqual(FlowStatusEnum.FLOW_COMPLETE.getValue(), processStatus)) {
            // 更新项目节点状态
            projectComponent.updateNodeStatus(projectId);
            return;
        }

        ConclusionVar conclusionVar = Optional.ofNullable(processInfo.getFlowData())
                .flatMap(flowData -> Optional.ofNullable(BeanUtil.toBean(flowData, ConclusionVar.class)))
                .orElseThrow(() -> {
                    log.error("[ProjectFlowComponentImpl.conclusionComplete]审批信息为空 processInstanceId:{}", processInstanceId);
                    return new BaseBizRuntimeException("审批信息为空");
                });

        // 更新普通评价
        updateEvaluate(projectDO, conclusionVar);

        // 获取更新结项日期,项目状态（取放在flowData中的数据）
        TargetStatusModel targetStatusModel = JSONObject.parseObject(projectFlowDO.getFlowData(), TargetStatusModel.class);
        Date conclusionDate = new Date();
        Integer oldStatus = projectDO.getStatus();
        Integer newStatus = targetStatusModel.getTargetStatus();

        // 组装，更新项目数据
        ProjectDO updateDO = new ProjectDO();
        updateDO.setId(projectId);
        updateDO.setStatus(newStatus);
        updateDO.setConclusionDate(conclusionDate);
        updateDO.setNodeStatus(ProjectNodeStatusEnum.CONCLUSION.getCode());
        projectMapper.update(updateDO);

        // 结项流程日志处理
        logComponent.addConclusion(projectId, oldStatus, newStatus, conclusionDate);

        // 如果项目状态为中止，需要执行中止逻辑
        if (ProjectStatusEnum.INVALID.getCode().equals(newStatus)) {
            invalid(projectId, targetStatusModel.getInvalidReason());
        }
    }

    /**
     * 项目作废处理
     *
     * @param projectId 项目id
     */
    private void invalid(Long projectId, String invalidReason) {

        final Integer status = ProjectStatusEnum.INVALID.getCode();

        // 修改中止原因， 添加中止原因日志
        ProjectDO updateReason = new ProjectDO();
        updateReason.setId(projectId);
        updateReason.setInvalidReason(invalidReason);
        projectMapper.update(updateReason);
        logComponent.addLogWhenContentChange("", invalidReason, projectId, BizChangeLogFieldEnum.TERMINATE_REASON.getText());

        //修改产品需求状态
        productDemandComponent.updateProductDemandStatus(projectId, status);

        // 作废解除关联
        projectProductDemandComponent.update(projectId, null);
        bizLabelComponent.deleteLabel(projectId, BizTypeEnum.PROJECT.getCode());

        // 更新任务状态
        taskComponent.updateStatusAsProjectStatusChange(projectId, status, false);

        // 刷新客开
        projectComponent.updateCustomDev(projectId);
    }

    private void updateEvaluate(ProjectDO projectDO, ConclusionVar conclusionVar) {
        Long projectId = projectDO.getId();

        Collection<ProjectEvaluateVar> reviewerEvaluates = conclusionVar.getReviewerEvaluates();
        for (ProjectEvaluateVar reviewerEvaluate : reviewerEvaluates) {
            ProjectEvaluateDO evaluateDO = new ProjectEvaluateDO();
            evaluateDO.setProjectId(projectId);
            evaluateDO.setReviewerScores(reviewerEvaluate.getScores());
            evaluateDO.setReviewerScoresDesc(reviewerEvaluate.getScoresDesc());
            evaluateDO.setEvaluateDimensionId(reviewerEvaluate.getDimensionId());
            evaluateMapper.update(evaluateDO);
        }
    }

}
