package com.timevale.forward.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.EvaluateDimensionMapper;
import com.timevale.forward.dal.dao.ProjectEvaluateMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMemberEvaluateMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectEvaluateService;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.ProjectKindEnum;
import com.timevale.forward.model.enums.ProjectLevelEnum;
import com.timevale.forward.service.copy.ProjectEvaluateCopier;
import com.timevale.forward.service.copy.ProjectMemberEvaluateCopier;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RestService
public class ProjectEvaluateServiceImpl implements ProjectEvaluateService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectEvaluateMapper evaluateMapper;
    @Resource
    private EvaluateDimensionMapper dimensionMapper;
    @Resource
    private ProjectMemberEvaluateMapper memberEvaluateMapper;

    @Override
    public BaseResult<ProjectMemberEvaluateVO> memberList(Long projectId) {
        // 查询并转换
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);
        List<MemberEvaluateVO> memberEvaluateVOList = ProjectMemberEvaluateCopier.INSTANCE.do2vo(memberEvaluateDOList);

        // 组装结构
        ProjectMemberEvaluateVO result = new ProjectMemberEvaluateVO();

        // 成员
        result.setMemberEvaluateVOList(memberEvaluateVOList);

        // 工作量总和
        BigDecimal planWorkLoadSum = memberEvaluateVOList.stream()
                .map(MemberEvaluateVO::getPlanWorkLoad)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setPlanWorkloadSum(planWorkLoadSum);

        // 需要计算积分的工作量总和
        BigDecimal workloadPointsSum = memberEvaluateVOList.stream()
                .filter(MemberEvaluateVO::getIncludeStat)
                .map(MemberEvaluateVO::getPlanWorkLoad).reduce(BigDecimal.ZERO, BigDecimal::add);
        result.setPointsWorkloadSum(workloadPointsSum);

        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Boolean> updateMemberEvaluate(MemberEvaluateModifyReq req) {
        ProjectMemberEvaluateDO evaluateDO = ProjectMemberEvaluateCopier.INSTANCE.req2do(req);
        memberEvaluateMapper.update(evaluateDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> memberWorkloadFill(MemberWorkloadFillReq req) {
        Long projectId = req.getProjectId();
        for (MemberWorkloadModifyReq modifyReq : req.getModifyReqList()) {
            ProjectMemberEvaluateDO evaluateDO = ProjectMemberEvaluateCopier.INSTANCE.req2do(modifyReq, projectId);
            memberEvaluateMapper.update(evaluateDO);
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectWorkloadChangeVO> workloadChangeCheck(MemberWorkloadFillReq req) {
        Long projectId = req.getProjectId();

        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO,"项目不存在");

        ProjectWorkloadChangeVO changeVO = ProjectMemberEvaluateCopier.INSTANCE.do2vo(projectDO);

        return null;
    }

    @Override
    public BaseResult<ProjectEvaluateVO> evaluateList(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        List<ProjectEvaluateDO> evaluateDOList = evaluateMapper.selectByProjectId(projectId);

        // 获取对应的维度
        List<Long> dimensionIdList = evaluateDOList.stream()
                .map(ProjectEvaluateDO::getEvaluateDimensionId)
                .collect(Collectors.toList());

        // 维度Map
        List<EvaluateDimensionDO> dimensionDOList = dimensionMapper.selectByIds(dimensionIdList);
        ImmutableMap<Long, EvaluateDimensionDO> dimensionMap = Maps.uniqueIndex(dimensionDOList, BaseDO::getId);

        // 转换评价项，填充数据
        List<ProjectEvaluateItemVO> evaluateItemVOList = new ArrayList<>();
        for (ProjectEvaluateDO evaluateDO : evaluateDOList) {
            EvaluateDimensionDO dimensionDO = dimensionMap.get(evaluateDO.getEvaluateDimensionId());
            ProjectEvaluateItemVO evaluateItemVO = ProjectEvaluateCopier.INSTANCE.do2vo(evaluateDO, dimensionDO);
            evaluateItemVOList.add(evaluateItemVO);
        }

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
    public BaseResult<Boolean> evaluateUpdate(ProjectEvaluateReq req) {
        List<EvaluateReq> evaluateReqList = req.getEvaluateReqList();
        List<ProjectEvaluateDO> evaluateDOList = ProjectEvaluateCopier.INSTANCE.req2do(evaluateReqList);

        for (ProjectEvaluateDO evaluateDO : evaluateDOList) {
            evaluateMapper.update(evaluateDO);
        }

        return BaseResult.success(true);
    }
}
