package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.encourage.facade.api.response.GetProjectPointResponse;
import com.timevale.encourage.facade.api.response.meta.UserPoint;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectEvaluateService;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectEvaluateComponent;
import com.timevale.forward.service.component.UserComponent;
import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.copy.ProjectEvaluateCopier;
import com.timevale.forward.service.copy.ProjectMemberEvaluateCopier;
import com.timevale.forward.service.flow.ForwardFlow;
import com.timevale.forward.service.integration.encourage.EncourageClient;
import com.timevale.forward.service.integration.epeius.EpeiusClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPermissionClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.RoleResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RestService
@RequiredArgsConstructor
public class ProjectEvaluateServiceImpl implements ProjectEvaluateService {
    private final ForwardFlow forwardFlow;
    private final EpeiusClient epeiusClient;
    final private CommonConfig commonConfig;
    final private UserComponent userComponent;
    private final ProjectMapper projectMapper;
    private final PersonComponent personComponent;
    private final EncourageClient encourageClient;
    private final HistoryRecordMapper recordMapper;
    private final ProjectFlowMapper projectFlowMapper;
    private final ProjectEvaluateMapper evaluateMapper;
    private final EvaluateDimensionMapper dimensionMapper;
    private final InnerUserPersonClient innerUserPersonClient;
    private final ProjectMemberEvaluateMapper memberEvaluateMapper;
    private final ProjectEvaluateComponent projectEvaluateComponent;
    private final InnerUserPermissionClient innerUserPermissionClient;

    @Override
    public BaseResult<ProjectMemberEvaluateVO> memberList(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        // 查询权限控制
        boolean allowVisitAllData = allowVisitAllData(projectId);
        boolean isMember = personComponent.exist(LocalSessionUtils.getUserInfo().getId(), projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        if (!allowVisitAllData && (!isMember || !ProjectStatusEnum.CONCLUSION.getCode().equals(projectDO.getStatus()))) {
            return BaseResult.success();
        }

        // 查询并转换
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.getByProjectId(projectId);
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

        // 结项流程, 权限控制
        String workloadFlowId = "";
        String conclusionFlowId = "";
        boolean conclusionAuditing = false;
        if (allowVisitAllData) {
            List<ProjectFlowDO> conclusionFlowList = projectFlowMapper.getByProjectIdAndType(projectId, FlowTypeEnum.CONCLUSION.getCode());
            String conclusionPid = conclusionFlowList.stream()
                    .max(Comparator.comparing(BaseDO::getId))
                    .map(ProjectFlowDO::getFlowId)
                    .orElse("");
            conclusionFlowId = Optional.ofNullable(epeiusClient.getProcessInfo(conclusionPid))
                    .flatMap(e -> Optional.ofNullable(e.getCurrentTaskIdList()))
                    .flatMap(e -> Optional.ofNullable(CollUtil.getLast(e)))
                    .orElse("");
            conclusionAuditing = conclusionFlowList.stream()
                    .map(ProjectFlowDO::getStatus)
                    .anyMatch(ForwardFlowStatusEnum.AUDITING.getCode()::equals);

            // 工作流变更流程，查询审核中的流程
            List<ProjectFlowDO> workloadFlowList = projectFlowMapper.getByProjectIdAndType(projectId, FlowTypeEnum.WORKLOAD.getCode());
            String workloadFlowPid = workloadFlowList.stream()
                    .filter(e -> ForwardFlowStatusEnum.AUDITING.getCode().equals(e.getStatus()))
                    .map(ProjectFlowDO::getFlowId)
                    .findFirst()
                    .orElse("");
            workloadFlowId = Optional.ofNullable(epeiusClient.getProcessInfo(workloadFlowPid))
                    .flatMap(e -> Optional.ofNullable(e.getCurrentTaskIdList()))
                    .flatMap(e -> Optional.ofNullable(CollUtil.getLast(e)))
                    .orElse("");
        }

        // 只能看到自己的积分
        if (!allowVisitAllData) {
            String userId = LocalSessionUtils.getUserInfo().getId();
            memberEvaluateVOList.removeIf(e-> !Objects.equals(userId, e.getUserId()));
        }

        // 查询激励系统积分
        Optional<GetProjectPointResponse> projectPointOpt = encourageClient.getProjectPoint(projectId);
        projectPointOpt.map(GetProjectPointResponse::getUserPoints)
                       .map(userPoints -> Maps.uniqueIndex(userPoints, UserPoint::getAccount))
                       .ifPresent(userPointMap -> memberEvaluateVOList.forEach(memberEvaluateVO -> {
                           UserPoint userPoint = userPointMap.get(memberEvaluateVO.getUserId());
                           if (userPoint != null) {
                               // 填充成员实得积分
                               memberEvaluateVO.setPersonalPoints(userPoint.getPersonalPoint());
                           }
                       }));

        // 组装数据
        ProjectMemberEvaluateVO result = new ProjectMemberEvaluateVO();
        result.setWorkloadFlowId(workloadFlowId);
        result.setPlanWorkloadSum(planWorkLoadSum);
        result.setConclusionFlowId(conclusionFlowId);
        result.setPointsWorkloadSum(workloadPointsSum);
        result.setConclusionAuditing(conclusionAuditing);
        result.setMemberEvaluateVOList(memberEvaluateVOList);
        projectPointOpt.map(GetProjectPointResponse::getProjectOriginalPoint).ifPresent(result::setProjectOriginalPoint);

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
        final Long projectId = req.getProjectId();
        ProjectWorkloadChangeVO changeVO = projectEvaluateComponent.workloadChangeForm(req);

        if (changeVO.getDirectChangeEnable()) {
            // 遍历修改参数，落库
            List<MemberWorkloadModifyReq> modifyReqList = req.getModifyReqList();
            for (MemberWorkloadModifyReq modifyReq : modifyReqList) {
                ProjectMemberEvaluateDO evaluateDO = ProjectMemberEvaluateCopier.INSTANCE.req2do(modifyReq, projectId);
                memberEvaluateMapper.updatePlanWorkload(evaluateDO);
            }

            // 判断是否存在基线版本，如果是则需要添加新版本
            if (recordMapper.selectLast(projectId) != null) {
                projectEvaluateComponent.additionRecord(projectId);
            }
        } else {
            //流程id
            String flowId = forwardFlow.workloadChangeFlow.start(changeVO, req);

            // 存储变更信息
            List<MemberWorkloadModifyReq> modifyReqList = req.getModifyReqList();
            String modifyWorkloadJson = JSON.toJSONString(modifyReqList);

            // 获取用户信息
            UserInfo userInfo = LocalSessionUtils.getUserInfo();

            // 添加工作流信息
            ProjectFlowDO projectFlowDO = new ProjectFlowDO()
                    .setFlowId(flowId)
                    .setProjectId(projectId)
                    .setFlowData(modifyWorkloadJson)
                    .setProposerId(userInfo.getId())
                    .setProposer(userInfo.getFullAlias())
                    .setFlowType(FlowTypeEnum.WORKLOAD.getCode())
                    .setStatus(ForwardFlowStatusEnum.AUDITING.getCode());
            projectFlowMapper.insert(projectFlowDO);
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> memberWorkloadCopy(Long projectId) {
        List<ProjectMemberEvaluateDO> evaluateDOList = memberEvaluateMapper.getByProjectId(projectId);

        // 计划工作量覆盖实际工作量
        for (ProjectMemberEvaluateDO evaluateDO : evaluateDOList) {
            ProjectMemberEvaluateDO updateDO = new ProjectMemberEvaluateDO();
            updateDO.setUserId(evaluateDO.getUserId());
            updateDO.setProjectId(evaluateDO.getProjectId());
            updateDO.setActualWorkload(evaluateDO.getPlanWorkload());
            memberEvaluateMapper.update(updateDO);
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

        // 权限控制
        boolean allowVisitAllData = allowVisitAllData(projectId);
        boolean isMember = personComponent.exist(LocalSessionUtils.getUserInfo().getId(), projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        if (!allowVisitAllData && (!isMember || !ProjectStatusEnum.CONCLUSION.getCode().equals(projectDO.getStatus()))) {
            return BaseResult.success();
        }

        // 获取SR建议评价等级
        Integer srEvaluateGrade = projectDO.getSrEvaluateGrade();
        String srEvaluateGradeName = GradeEnum.getTextByCode(srEvaluateGrade);

        // 查询对应的项目评价，旧数据判空处理
        List<ProjectEvaluateDO> evaluateDOList = evaluateMapper.getByProjectId(projectId);
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

        // 计算项目评价总分
        BigDecimal scoresSum = evaluateItemVOList.stream()
                .map(ProjectEvaluateItemVO::getReviewerScores)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal::add)
                .orElse(null);

        // 查询激励系统积分
        Optional<GetProjectPointResponse> projectPointOpt = encourageClient.getProjectPoint(projectId);

        // 组装结果
        ProjectEvaluateVO result = new ProjectEvaluateVO();
        result.setScoresSum(scoresSum);
        result.setSrEvaluateGrade(srEvaluateGrade);
        result.setSrEvaluateGradeName(srEvaluateGradeName);
        result.setEvaluateItemVOList(evaluateItemVOList);
        projectPointOpt.map(GetProjectPointResponse::getProjectPoint).ifPresent(result::setProjectPoint);

        return BaseResult.success(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> evaluateUpdate(ProjectEvaluateReq req) {
        List<EvaluateReq> evaluateReqList = req.getEvaluateReqList();
        List<ProjectEvaluateDO> evaluateDOList = ProjectEvaluateCopier.INSTANCE.req2do(evaluateReqList);

        for (ProjectEvaluateDO evaluateDO : evaluateDOList) {
            evaluateMapper.update(evaluateDO);
            evaluateMapper.updateScores(evaluateDO);
        }

        // 更新 sr 评价等级
        Optional<Long> projectIdOpt = evaluateDOList.stream().map(ProjectEvaluateDO::getProjectId).findAny();
        if (projectIdOpt.isPresent()) {
            Long projectId = projectIdOpt.get();
            Optional.ofNullable(req.getSrEvaluateGrade())
                    .ifPresent(e -> {
                        ProjectDO projectDO = new ProjectDO();
                        projectDO.setId(projectId);
                        projectDO.setSrEvaluateGrade(e);
                        projectMapper.update(projectDO);
                    });
        }

        return BaseResult.success(true);
    }

    /**
     * 是否允许访问所有数据
     *
     * @param projectId 项目id
     * @return boolean
     */
    private boolean allowVisitAllData(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            return false;
        }

        String pmId = projectDO.getPmId();
        String srId = projectDO.getSrId();
        String principalId = projectDO.getPrincipalId();
        String otnPrincipalId = projectDO.getOtnPrincipalId();

        // （产品经理 + 项目经理 + sr ）的上级
        Set<String> subordinateIds = new HashSet<>();
        subordinateIds.add(pmId);
        subordinateIds.add(srId);

        List<PersonDO> pds = personComponent.select(projectId, PersonTypeEnum.PROJECT_PD.getCode());
        Set<String> pdIds = pds.stream().map(PersonDO::getUserId).collect(Collectors.toSet());
        subordinateIds.addAll(pdIds);

        List<String> superiorIds = innerUserPersonClient.getDefaultSuperior(subordinateIds, true);

        // PMO
        List<String> allPMOIds = userComponent.getAllPmo(commonConfig.getEvalPmoGroup());

        Set<String> permissionIds = new HashSet<>();
        permissionIds.add(pmId);
        permissionIds.add(srId);
        permissionIds.add(principalId);
        permissionIds.add(otnPrincipalId);
        permissionIds.addAll(pdIds);
        permissionIds.addAll(allPMOIds);
        permissionIds.addAll(superiorIds);

        permissionIds.addAll(StrUtil.split(commonConfig.getAllowVisitAllDataAccount(), ','));
        CollUtil.removeEmpty(permissionIds);

        // 判断当前用户是否为以上的权限用户
        String userId = LocalSessionUtils.getUserInfo().getId();
        if (permissionIds.contains(userId)) {
            return true;
        } else {
            List<RoleResponse> myRoles = innerUserPermissionClient.getFunctionRoleInfo(userId);
            List<String> evaluateRoles = StrUtil.split(commonConfig.getEvaluateManagerRoleId(), ',');
            return myRoles.stream()
                    .map(RoleResponse::getId)
                    .anyMatch(evaluateRoles::contains);
        }
    }
}
