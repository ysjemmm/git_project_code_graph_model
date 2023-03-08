package com.timevale.forward.service.component;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.epeius.service.model.request.StartProcessRequest;
import com.timevale.forward.dal.dao.EvaluateDimensionMapper;
import com.timevale.forward.dal.dao.ProjectEvaluateMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMemberEvaluateMapper;
import com.timevale.forward.dal.entity.EvaluateDimensionDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectEvaluateDO;
import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import com.timevale.forward.model.enums.MessageTagEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectEvaluateCopier;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.epeius.model.ConclusionVar;
import com.timevale.forward.service.integration.epeius.model.ProjectEvaluateVar;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
    private ProjectMapper projectMapper;
    @Resource
    private ProjectComponent projectComponent;
    @Resource
    private EpeiusClient epeiusClient;
    @Resource
    private ProjectEvaluateMapper evaluateMapper;
    @Resource
    private EvaluateDimensionMapper dimensionMapper;
    @Resource
    private ProjectMemberEvaluateMapper memberEvaluateMapper;

    public String conclusionFlow(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 当前用户发起人id
        String startAccountId = LocalSessionUtils.getUserInfo().getId();


        // 项目地址
        String projectUrl = projectComponent.getUrl(projectId);

        // 查询当前PMO
        List<String> pmoIdList = CollUtil.newArrayList("yangxu","shanluo");

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
                    // 发起人是否为项目负责人或者1-n产研负责人，直接把评价信息带入审批信息里面
                    if (startIsPrincipal) {
                        return e;
                    } else {
                        ProjectEvaluateVar evaluateVar = new ProjectEvaluateVar();
                        evaluateVar.setDimensionName(e.getDimensionName());
                        return evaluateVar;
                    }
                })
                .collect(Collectors.toList());

        // 计划总工作量、工作量计算积分
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);
        String planWorkloadSum = memberEvaluateDOList.stream()
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .toString();
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
}
