package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectEvaluateService;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.FlowTypeEnum;
import com.timevale.forward.model.enums.ForwardFlowStatusEnum;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.ProjectLevelEnum;
import com.timevale.forward.service.component.ProjectEvaluateComponent;
import com.timevale.forward.service.component.WorkFlowComponent;
import com.timevale.forward.service.copy.ProjectEvaluateCopier;
import com.timevale.forward.service.copy.ProjectMemberEvaluateCopier;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RequiredArgsConstructor
public class ProjectEvaluateServiceImpl implements ProjectEvaluateService {
    private final ProjectMapper projectMapper;
    private final ProjectFlowMapper projectFlowMapper;
    private final WorkFlowComponent workFlowComponent;
    private final ProjectEvaluateMapper evaluateMapper;
    private final EvaluateDimensionMapper dimensionMapper;
    private final ProjectMemberEvaluateMapper memberEvaluateMapper;
    private final ProjectEvaluateComponent projectEvaluateComponent;

    @Override
    public BaseResult<ProjectMemberEvaluateVO> memberList(Long projectId) {
        // 查询并转换
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);
        List<MemberEvaluateVO> memberEvaluateVOList = ProjectMemberEvaluateCopier.INSTANCE.do2vo(memberEvaluateDOList);

        // 工作量总和
        BigDecimal planWorkLoadSum = memberEvaluateVOList.stream()
                .map(MemberEvaluateVO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 需要计算积分的工作量总和
        BigDecimal workloadPointsSum = memberEvaluateVOList.stream()
                .filter(MemberEvaluateVO::getIncludeStat)
                .map(MemberEvaluateVO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 结项流程, 查询审批中、或者审核通过的流程
        List<ProjectFlowDO> conclusionFlowList = projectFlowMapper.getByProjectIdAndType(projectId, FlowTypeEnum.CONCLUSION.getCode());
        String conclusionFlowId = conclusionFlowList.stream()
                .filter(e -> ForwardFlowStatusEnum.COMPLETE.getCode().equals(e.getStatus())
                        || ForwardFlowStatusEnum.AUDITING.getCode().equals(e.getStatus()))
                .map(ProjectFlowDO::getFlowId)
                .findAny()
                .orElse("");

        // 工作流变更流程，查询审核中的流程
        List<ProjectFlowDO> workloadFlowList = projectFlowMapper.getByProjectIdAndType(projectId, FlowTypeEnum.WORKLOAD.getCode());
        String workloadFlowId = workloadFlowList.stream()
                .filter(e -> ForwardFlowStatusEnum.AUDITING.getCode().equals(e.getStatus()))
                .map(ProjectFlowDO::getFlowId)
                .findAny()
                .orElse("");

        // 组装数据
        ProjectMemberEvaluateVO result = new ProjectMemberEvaluateVO();
        result.setWorkloadFlowId(workloadFlowId);
        result.setPlanWorkloadSum(planWorkLoadSum);
        result.setConclusionFlowId(conclusionFlowId);
        result.setPointsWorkloadSum(workloadPointsSum);
        result.setMemberEvaluateVOList(memberEvaluateVOList);

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Boolean> updateMemberEvaluate(MemberEvaluateModifyReq req) {
        ProjectMemberEvaluateDO evaluateDO = ProjectMemberEvaluateCopier.INSTANCE.req2do(req);
        memberEvaluateMapper.update(evaluateDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> memberWorkloadFill(MemberWorkloadFillReq req) {
        // 校验并获取表单信息
        ProjectWorkloadChangeVO changeVO = projectEvaluateComponent.workloadChangeForm(req);

        if (changeVO.getDirectChangeEnable()) {
            // 遍历修改参数，落库
            Long projectId = req.getProjectId();
            List<MemberWorkloadModifyReq> modifyReqList = req.getModifyReqList();
            for (MemberWorkloadModifyReq modifyReq : modifyReqList) {
                ProjectMemberEvaluateDO evaluateDO = ProjectMemberEvaluateCopier.INSTANCE.req2do(modifyReq, projectId);
                memberEvaluateMapper.update(evaluateDO);
            }
        } else {
            // 项目id 及 流程id
            Long projectId = changeVO.getProjectId();
            String flowId = workFlowComponent.workloadChangeFlow(changeVO, req);

            // 存储变更信息
            List<MemberWorkloadModifyReq> modifyReqList = req.getModifyReqList();
            String modifyWorkloadJson = JSON.toJSONString(modifyReqList);

            // 添加工作流信息
            ProjectFlowDO projectFlowDO = new ProjectFlowDO()
                    .setFlowId(flowId)
                    .setProjectId(projectId)
                    .setFlowData(modifyWorkloadJson)
                    .setFlowType(FlowTypeEnum.WORKLOAD.getCode())
                    .setStatus(ForwardFlowStatusEnum.AUDITING.getCode());
            projectFlowMapper.insert(projectFlowDO);
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectWorkloadChangeVO> workloadChangeCheck(MemberWorkloadFillReq req) {
        return BaseResult.success(projectEvaluateComponent.workloadChangeForm(req));
    }

    @Override
    public BaseResult<ProjectEvaluateVO> evaluateList(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 查询对应的项目评价，旧数据判空处理
        List<ProjectEvaluateDO> evaluateDOList = evaluateMapper.selectByProjectId(projectId);
        if (CollUtil.isEmpty(evaluateDOList)) {
            ProjectEvaluateVO result = new ProjectEvaluateVO();
            result.setEvaluateItemVOList(new ArrayList<>());
            return BaseResult.success(result);
        }

        // 获取对应的维度
        List<Long> dimensionIdList = evaluateDOList.stream()
                .map(ProjectEvaluateDO::getEvaluateDimensionId)
                .collect(Collectors.toList());

        // 维度Map
        List<EvaluateDimensionDO> dimensionDOList = dimensionMapper.selectByIds(dimensionIdList);
        ImmutableMap<Long, EvaluateDimensionDO> dimensionMap = Maps.uniqueIndex(dimensionDOList, BaseDO::getId);

        // 转换评价项，填充数据
        List<ProjectEvaluateItemVO> evaluateItemVOList = evaluateDOList.stream()
                .map(e -> ProjectEvaluateCopier.INSTANCE.do2item(e, dimensionMap.get(e.getEvaluateDimensionId())))
                .collect(Collectors.toList());

        // PBG项目/基线项目，且项目等级≠B取PMO评价各维度分数之和；其他，取各维度评分数之和
        boolean selectPMO = ObjectUtil.equals(ProjectKindEnum.PBG_BASE.getCode(), projectDO.getKind())
                && ObjectUtil.notEqual(ProjectLevelEnum.B.getCode(), projectDO.getLevel());

        // 计算项目评价总分
        Integer scoresSum = evaluateItemVOList.stream()
                .map(e -> selectPMO ? e.getPmoScores() : e.getScores())
                .filter(ObjectUtil::isNotNull)
                .reduce(Integer::sum)
                .orElse(null);

        // 组装结果
        ProjectEvaluateVO result = new ProjectEvaluateVO();
        result.setScoresSum(scoresSum);
        result.setEvaluateItemVOList(evaluateItemVOList);

        return BaseResult.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> evaluateUpdate(ProjectEvaluateReq req) {
        List<EvaluateReq> evaluateReqList = req.getEvaluateReqList();
        List<ProjectEvaluateDO> evaluateDOList = ProjectEvaluateCopier.INSTANCE.req2do(evaluateReqList);

        for (ProjectEvaluateDO evaluateDO : evaluateDOList) {
            evaluateMapper.update(evaluateDO);
        }

        return BaseResult.success(true);
    }
}
