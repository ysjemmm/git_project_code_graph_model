package com.timevale.forward.service.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.epeius.service.enums.FlowStatusEnum;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectEvaluateCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.epeius.model.ConclusionVar;
import com.timevale.forward.service.integration.epeius.model.ProjectEvaluateVar;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.lowcode.support.response.process.ProcessResponse;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/03/08 18:00
 */
@Slf4j
@Component
public class WorkFlowComponent {
    @Resource
    private EpeiusClient epeiusClient;
    @Resource
    private UserComponent userComponent;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectComponent projectComponent;
    @Resource
    private ProjectFlowMapper projectFlowMapper;
    @Resource
    private ProjectEvaluateMapper evaluateMapper;
    @Resource
    private EvaluateDimensionMapper dimensionMapper;
    @Resource
    private ProjectMemberEvaluateMapper memberEvaluateMapper;

    @Value("${conclusionPmoGroup:557300580")
    private String CONCLUSION_PMO_GROUP;

    /**
     * 结项工作流——发起
     *
     * @param projectId 项目id
     * @return {@link String}
     */
    public String conclusionFlow(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 当前用户发起人id
        String startAccountId = LocalSessionUtils.getUserInfo().getId();

        // 项目地址
        String projectUrl = projectComponent.getUrl(projectId);

        // 查询结项流程PMO
        List<String> pmoIdList = userComponent.getPmo(CONCLUSION_PMO_GROUP);

        // 发起人是否为项目负责人或者1-n产研负责人
        Set<String> principalIdSet = CollUtil.newHashSet(projectDO.getPrincipalId(), projectDO.getOtnPrincipalId());
        List<String> principalIdList = CollUtil.newArrayList(principalIdSet);
        boolean startIsPrincipal = principalIdSet.contains(startAccountId);
        String isPrincipal = YesOrNoEnum.getTextByCode(startIsPrincipal);

        // 项目评价信息
        List<ProjectEvaluateDO> evaluateDOList = evaluateMapper.selectByProjectId(projectId);
        List<EvaluateDimensionDO> dimensionDOList = dimensionMapper.selectByKind(projectDO.getKind());
        ImmutableMap<Long, EvaluateDimensionDO> dimensionDOMap = Maps.uniqueIndex(dimensionDOList, EvaluateDimensionDO::getId);

        // 项目评价参数
        List<ProjectEvaluateVar> evaluateVarList = evaluateDOList.stream()
                .map(e -> ProjectEvaluateCopier.INSTANCE.do2var(e, dimensionDOMap.get(e.getEvaluateDimensionId())))
                .collect(Collectors.toList());

        // 审批人评价参数
        List<ProjectEvaluateVar> reviewerEvaluateVarList = evaluateVarList.stream()
                .map(e -> {
                    // 发起人是否为项目负责人或者1-n产研负责人，带入全部信息，否则只带入评价维度信息
                    if (startIsPrincipal) {
                        return e;
                    } else {
                        return new ProjectEvaluateVar()
                                .setDimensionId(e.getDimensionId())
                                .setDimensionName(e.getDimensionName());
                    }
                })
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
        conclusionVar.setPMOIdList(pmoIdList);
        conclusionVar.setProjectUrl(projectUrl);
        conclusionVar.setIsPrincipal(isPrincipal);
        conclusionVar.setEvaluateList(evaluateVarList);
        conclusionVar.setPrincipalIdList(principalIdList);
        conclusionVar.setPlanWorkloadSum(planWorkloadSum);
        conclusionVar.setPointsWorkloadSum(pointsWorkloadSum);
        conclusionVar.setReviewerEvaluateList(reviewerEvaluateVarList);

        // 转换为Map
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
    public void conclusionComplete(String processInstanceId) {
        AssertUtil.checkState(StrUtil.isNotBlank(processInstanceId), "流程id为空");

        // 获取项目流程信息
        ProjectFlowDO projectFlowDO = projectFlowMapper.getByFlowId(processInstanceId);
        AssertUtil.notNull(projectFlowDO, "流程不存在");

        // 查询对应项目
        ProjectDO projectDO = projectMapper.get(projectFlowDO.getProjectId());
        AssertUtil.notNull(projectFlowDO, "项目不存在");

        // 获取流程信息
        ProcessResponse processInfo  = epeiusClient.getProcessInfo(processInstanceId);
        AssertUtil.notNull(processInfo, "流程信息为空");

        // 更新流程状态
        String processStatus = processInfo.getProcessStatus();
        Optional.ofNullable(ForwardFlowStatusEnum.getByValue(processStatus))
                .flatMap(e -> Optional.ofNullable(e.getCode()))
                .ifPresent(e -> projectFlowMapper.updateStatus(projectFlowDO.getId(), e));


        // 只有审批通过，需要更新项目评价信息
        if (ObjectUtil.notEqual(FlowStatusEnum.FLOW_COMPLETE.getValue(), processStatus)) {
            return;
        }

        // 取出评价信息, 更新
        List<ProjectEvaluateVar> evaluateVarList = Optional.ofNullable(processInfo.getFlowData())
                .flatMap(flowData -> Optional.ofNullable(BeanUtil.toBean(flowData, ConclusionVar.class)))
                .flatMap(conclusionVar -> Optional.ofNullable(conclusionVar.getReviewerEvaluateList()))
                .orElse(null);

        if (evaluateVarList != null) {
            for (ProjectEvaluateVar evaluateVar : evaluateVarList) {
                // 需要更新的数据
                ProjectEvaluateDO updateEvaluate = new ProjectEvaluateDO();
                updateEvaluate.setProjectId(projectDO.getId());
                updateEvaluate.setEvaluateDimensionId(evaluateVar.getDimensionId());

                // 基线且非迭代，更新PMO评价，否在更新项目评价
                if (ObjectUtil.equal(ProjectKindEnum.PBG_BASE.getCode(), projectDO.getKind())
                        && ObjectUtil.notEqual(ProjectTypeEnum.RENEW.getCode(), projectDO.getType())) {
                    updateEvaluate.setPmoScores(evaluateVar.getScores());
                    updateEvaluate.setPmoScoresDesc(evaluateVar.getScoresDesc());
                } else {
                    updateEvaluate.setScores(evaluateVar.getScores());
                    updateEvaluate.setScoresDesc(evaluateVar.getScoresDesc());
                }

                // 更新落库
                evaluateMapper.update(updateEvaluate);
            }
        } else {
            log.error("[ProjectFlowComponentImpl.conclusionComplete]审批信息为空 processInstanceId:{}", processInstanceId);
        }
    }


}
