package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.request.MemberWorkloadFillReq;
import com.timevale.forward.facade.api.request.MemberWorkloadModifyReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.MemberEvaluateChangeVO;
import com.timevale.forward.facade.api.result.ProjectWorkloadChangeVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.copy.ProjectMemberEvaluateCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.exception.BaseIllegalStateException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.security.facade.enums.IncentiveMethodEnum;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @author by YangXu
 * @date 2023/03/10 14:27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectEvaluateComponent {
    private final TaskComponent taskComponent;
    private final ProjectMapper projectMapper;
    private final PersonComponent personComponent;
    private final HistoryRecordMapper recordMapper;
    private final ProjectFlowMapper projectFlowMapper;
    private final BizLabelComponent bizLabelComponent;
    private final ProjectEvaluateMapper evaluateMapper;
    private final ProjectLogComponent projectLogComponent;
    private final EvaluateDimensionMapper dimensionMapper;
    private final InnerUserPersonClient innerUserPersonClient;
    private final ProjectNodeFlowMapper projectNodeFlowMapper;
    private final ProductDemandComponent productDemandComponent;
    private final ProjectMemberEvaluateMapper memberEvaluateMapper;
    private final ProjectProductDemandComponent projectProductDemandComponent;

    /**
     * 添加工作量记录
     *
     * @param projectId 项目id
     */
    public void additionRecord(Long projectId) {
        additionRecord(projectId, null);
    }

    /**
     * 添加工作量记录
     *
     * @param projectId  项目id
     * @param userIdColl 仅需要记录的成员，为空则为全部成员
     */
    public void additionRecord(Long projectId, Collection<String> userIdColl) {
        // 查询最新版本，生成下一个版本号
        BigDecimal lastVersion = Optional.ofNullable(recordMapper.selectLast(projectId))
                .flatMap(e -> Optional.ofNullable(e.getVersion()))
                .orElse(BigDecimal.ZERO);
        BigDecimal newVersion = lastVersion.add(BigDecimal.ONE);

       // 查询该项目的成员
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.getByProjectId(projectId);

        // 过滤没有计划工作量的成员
        memberEvaluateDOList = memberEvaluateDOList.stream()
                .filter(e -> ObjectUtil.isNotNull(e.getPlanWorkload()))
                .collect(Collectors.toList());

        // 记录的内容, 过滤无需记录的成员
        if (CollUtil.isNotEmpty(userIdColl)) {
            memberEvaluateDOList = memberEvaluateDOList.stream()
                    .filter(e -> userIdColl.contains(e.getUserId()))
                    .collect(Collectors.toList());
        }

        // 转换为JSON
        String recordContent = JSON.toJSONString(memberEvaluateDOList);

        // 组装落库
        HistoryRecordDO recordDO = new HistoryRecordDO();
        recordDO.setVersion(newVersion);
        recordDO.setProjectId(projectId);
        recordDO.setRecordContent(recordContent);
        recordMapper.insert(recordDO);
    }

    /**
     * 工作量变更表单
     *
     * @param req 请求
     * @return {@link ProjectWorkloadChangeVO}
     */
    public ProjectWorkloadChangeVO workloadChangeForm(MemberWorkloadFillReq req) {
        final Long projectId = req.getProjectId();

        // 判断项目是否存在
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO,"项目不存在");

        // 判断是否存在审批流程
        List<ProjectFlowDO> auditingFlows = projectFlowMapper.getByStatus(projectId,
                FlowTypeEnum.WORKLOAD.getCode(),
                ForwardFlowStatusEnum.AUDITING.getCode());
        AssertUtil.checkState(CollUtil.isEmpty(auditingFlows), "已存在审核中的工作量变更流程");

        // 需要判断为是否1-n客开首次变更
        Optional<ProjectWorkloadChangeVO> changeFormOpt = otnWorkloadChangeForm(req);
        if (changeFormOpt.isPresent()) {
            return changeFormOpt.get();
        }

        // 不存在基线版本，直接返回，无需表单数据
        if (recordMapper.selectLast(projectId) == null) {
            return new ProjectWorkloadChangeVO().setDirectChangeEnable(true);
        }

        // 成员评价信息
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.getByProjectId(projectId);

        // 纳入积分的团队成员
        Set<String> includeStatUsers = memberEvaluateDOList.stream()
                .filter(ProjectMemberEvaluateDO::getIncludeStat)
                .map(ProjectMemberEvaluateDO::getUserId)
                .collect(Collectors.toSet());

        // 旧的计划总工作量、积分总工作量
        BigDecimal planWorkloadSumBefore = memberEvaluateDOList.stream()
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal pointsWorkloadSumBefore = memberEvaluateDOList.stream()
                .filter(e -> includeStatUsers.contains(e.getUserId()))
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        // 新的总计划工作量、积分总工作量
        List<MemberWorkloadModifyReq> memberWorkloadList = req.getModifyReqList();
        BigDecimal planWorkloadSumAfter = memberWorkloadList.stream()
                .map(MemberWorkloadModifyReq::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
        BigDecimal pointsWorkloadSumAfter = memberWorkloadList.stream()
                .filter(e -> includeStatUsers.contains(e.getUserId()))
                .map(MemberWorkloadModifyReq::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);

        // 新旧工作量差值，判断变更类型
        List<String> changeTypeList = new ArrayList<>();
        BigDecimal planWorkloadAddSum = BigDecimal.ZERO;
        BigDecimal pointsWorkloadAddSum = BigDecimal.ZERO;

        if (planWorkloadSumAfter.compareTo(planWorkloadSumBefore) > 0) {
            planWorkloadAddSum = planWorkloadSumAfter.subtract(planWorkloadSumBefore);
            changeTypeList.add(WorkloadChangeTypeEnum.PLAN_WORKLOAD_ADD.getText());
        }
        if (pointsWorkloadSumAfter.compareTo(pointsWorkloadSumBefore) > 0) {
            pointsWorkloadAddSum = pointsWorkloadSumAfter.subtract(pointsWorkloadSumBefore);
            changeTypeList.add(WorkloadChangeTypeEnum.POINTS_WORKLOAD_ADD.getText());
        }

        // 如果总工作量没有发生变化，直接返回，无需表单数据
        if (CollUtil.isEmpty(changeTypeList)) {
            return new ProjectWorkloadChangeVO().setDirectChangeEnable(true);
        }

        // 积分成员数据变化明细
        Map<String, BigDecimal> afterWorkLoadMap = memberWorkloadList.stream()
                .collect(Collectors.toMap(MemberWorkloadModifyReq::getUserId, MemberWorkloadModifyReq::getPlanWorkload, (a, b) -> a));
        List<MemberEvaluateChangeVO> memberEvaluateChanges = memberEvaluateDOList.stream().map(e -> new MemberEvaluateChangeVO()
                        .setUserName(e.getUserName())
                        .setPlanWorkloadBefore(e.getPlanWorkload())
                        .setPlanWorkloadAfter(afterWorkLoadMap.get(e.getUserId()))
                        .setIncludeStatName(YesOrNoEnum.getTextByCode(e.getIncludeStat())))
                .collect(Collectors.toList());

        // 填装表单数据
        ProjectWorkloadChangeVO result = ProjectMemberEvaluateCopier.INSTANCE.do2vo(projectDO);
        return result.setDirectChangeEnable(false)
                .setPlanWorkloadBefore(planWorkloadSumBefore)
                .setPlanWorkloadAfter(planWorkloadSumAfter)
                .setPointWorkloadBefore(pointsWorkloadSumBefore)
                .setPointWorkloadAfter(pointsWorkloadSumAfter)
                .setPlanWorkloadAddSum(planWorkloadAddSum)
                .setPointWorkloadAddSum(pointsWorkloadAddSum)
                .setChangeTypeList(changeTypeList)
                .setMemberEvaluateChanges(memberEvaluateChanges);
    }

    /**
     * 1-n客开工作量首次大于‘立项工作量评估’的数值
     *
     * @param req 请求
     * @return {@link ProjectWorkloadChangeVO}
     */
    private Optional<ProjectWorkloadChangeVO> otnWorkloadChangeForm(MemberWorkloadFillReq req) {
        final Long projectId = req.getProjectId();

        // 判断项目是否存在
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO,"项目不存在");

        // 判断是否存在审批流程
        List<ProjectFlowDO> flows = projectFlowMapper.getByProjectIdAndType(projectId, FlowTypeEnum.WORKLOAD.getCode());
        AssertUtil.checkState(flows.stream().noneMatch(e -> ForwardFlowStatusEnum.AUDITING.getCode().equals(e.getStatus())),
                "已存在审核中的工作量变更流程");

        // 非客开不走该流程
        if (!ProjectKindEnum.PBG_OTN.getCode().equals(projectDO.getKind())) {
            return Optional.empty();
        }
        // 立项工作量评估（人天)，如果不存在则不走该变更流程
        BigDecimal resourceAssessment = projectDO.getResourceAssessment();
        if (resourceAssessment == null) {
            return Optional.empty();
        }
        // 如果非首次变更，则不走该审批
        if (flows.stream().anyMatch(e -> ForwardFlowStatusEnum.COMPLETE.getCode().equals(e.getStatus()))) {
            return Optional.empty();
        }

        // 新的工作量
        List<MemberWorkloadModifyReq> memberWorkloadList = req.getModifyReqList();
        BigDecimal planWorkloadSumAfter = memberWorkloadList.stream()
                .map(MemberWorkloadModifyReq::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(1, RoundingMode.HALF_UP);
        // 新增的工作量
        BigDecimal planWorkloadAddSum = planWorkloadSumAfter.subtract(resourceAssessment);

        // 立项工作量评估（人天) 如果大于等于新的工作量总和则不需要审批
        if (planWorkloadAddSum.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        // 填装表单数据
        ProjectWorkloadChangeVO result = ProjectMemberEvaluateCopier.INSTANCE.do2vo(projectDO);
        result.setDirectChangeEnable(false)
                .setPlanWorkloadBefore(resourceAssessment)
                .setPlanWorkloadAfter(planWorkloadSumAfter)
                .setPlanWorkloadAddSum(planWorkloadAddSum)
                .setChangeTypeList(CollUtil.newArrayList(WorkloadChangeTypeEnum.PLAN_WORKLOAD_ADD.getText()));
        return Optional.of(result);
    }

    /**
     * 更新成员
     *
     * @param projectId  项目id
     * @param newMembers 新成员
     */
    public void addMember(Long projectId, Collection<PersonAddReq> newMembers) {
        // 当前积分团队成员
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.getByProjectId(projectId);
        Set<String> oldMemberIdSet = memberEvaluateDOList.stream()
                .map(ProjectMemberEvaluateDO::getUserId)
                .collect(Collectors.toSet());

        // 过滤掉可能存在的旧成员
        List<PersonAddReq> members = newMembers.stream().filter(e -> !oldMemberIdSet.contains(e.getUserId())).collect(Collectors.toList());

        // 新增成员
        addMemberNoCheck(projectId, members);
    }

    /**
     * 更新成员
     *
     * @param projectId  项目id
     * @param newMembers 新成员
     */
    public void updateMember(Long projectId, Collection<PersonAddReq> newMembers) {
        // 当前团队成员
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.getByProjectId(projectId);
        List<PersonAddReq> oldMembers = memberEvaluateDOList.stream()
                .map(e -> new PersonAddReq(e.getUserName(), e.getUserId()))
                .collect(Collectors.toList());

        // 分析出需要新增、删除的成员， 处理成员积分
        Collection<PersonAddReq> addMembers = CollUtil.subtract(newMembers, oldMembers);
        Collection<PersonAddReq> delMembers = CollUtil.subtract(oldMembers, newMembers);

        // 删除成员，如果存在审批中的工作量变更流程，不允许删除成员
        if (CollUtil.isNotEmpty(delMembers)) {
            List<ProjectFlowDO> auditingFlows = projectFlowMapper.getByStatus(projectId,
                    FlowTypeEnum.WORKLOAD.getCode(),
                    ForwardFlowStatusEnum.AUDITING.getCode());
            AssertUtil.checkState(CollUtil.isEmpty(auditingFlows), "存在审批中的工作量变更流程，不允许删除成员");

            // 删除落库
            List<String> delUserIdColl = delMembers.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
            memberEvaluateMapper.batchDelete(projectId, delUserIdColl);
        }

        // 新增成员
        addMemberNoCheck(projectId, addMembers);
    }

    /**
     * 同步成员
     *
     * @param projectId 项目id
     */
    public void syncMember(Long projectId) {
        // 项目成员
        List<PersonDO> projectMembers = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        List<PersonAddReq> newMembers = projectMembers.stream()
                .map(e -> new PersonAddReq(e.getUserName(), e.getUserId()))
                .collect(Collectors.toList());
        updateMember(projectId, newMembers);
    }

    /**
     * 添加积分成员，不做检查
     *
     * @param projectId 项目id
     * @param members   成员
     */
    private void addMemberNoCheck(Long projectId, Collection<PersonAddReq> members) {
        if (CollUtil.isEmpty(members)) {
            return;
        }

        // 查询内部用户中心，获取用户信息，用于判断是否纳入积分统计
        List<String> memberIds = members.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        List<BaseInfoResponse> membersInfo =  innerUserPersonClient.batchGetStaffInfos(memberIds, false);
        ImmutableMap<String, BaseInfoResponse> memberInfoMap = Maps.uniqueIndex(membersInfo, BaseInfoResponse::getAccount);

        List<ProjectMemberEvaluateDO> addEvalMembers = members.stream()
                .map(e -> {
                    boolean includeStat = false;

                    // 用户激励方式为积分制的时候，才纳入积分统计
                    final String userId = e.getUserId();
                    BaseInfoResponse memberInfo = memberInfoMap.get(userId);
                    if (memberInfo == null) {
                        log.warn("[ProjectEvaluateComponent.addMember]未查询到对应员工的信息:{}", userId);
                    } else {
                        Integer incentiveMethod = memberInfo.getIncentiveMethod();
                        includeStat = IncentiveMethodEnum.POINTS.getCode().equals(incentiveMethod);
                    }

                    // 转换
                    ProjectMemberEvaluateDO evaluateDO = ProjectMemberEvaluateCopier.INSTANCE.person2do(e);
                    evaluateDO.setProjectId(projectId);
                    evaluateDO.setIncludeStat(includeStat);
                    evaluateDO.setEvaluateGrade(GradeEnum.B.getCode());

                    return evaluateDO;
                })
                .collect(Collectors.toList());

        // 新增落库
        memberEvaluateMapper.batchInsert(addEvalMembers);
    }

    /**
     * 初始化项目评价
     *
     * @param projectId 项目id
     * @param kind      种类
     */
    public void initEvaluate(Long projectId, Integer kind) {
        List<EvaluateDimensionDO> dimensionDOList = dimensionMapper.selectByKindDate(kind, new Date());
        List<Long> dimensionIdList = dimensionDOList.stream().map(BaseDO::getId).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(dimensionIdList)) {
            evaluateMapper.batchInsert(projectId, dimensionIdList);
        }
    }

    /**
     * 更新项目维度评价处理
     *
     * @param projectId 项目id
     */
    public void updateEvalDimension(Long projectId) {
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        List<ProjectEvaluateDO> evaluateDOList = evaluateMapper.getByProjectId(projectId);
        Optional<Long> dimensionIdOpt = evaluateDOList.stream()
                .map(ProjectEvaluateDO::getEvaluateDimensionId)
                .findAny();

        dimensionIdOpt.ifPresent(dimensionId -> {
            List<EvaluateDimensionDO> dimensionDOList = dimensionMapper.selectById(dimensionId);
            Integer dimensionKind = dimensionDOList.stream()
                    .map(EvaluateDimensionDO::getKind)
                    .findAny()
                    .orElse(null);

            Integer projectKind = projectDO.getKind();
            if (ObjectUtil.notEqual(projectKind, dimensionKind)) {
                evaluateMapper.delete(projectId);
                initEvaluate(projectId, projectKind);
            }
        });

    }

    /**
     * 结项预检
     *
     * @param projectId 项目id
     */
    public void conclusionPreview(Long projectId) throws BaseIllegalStateException, BaseBizRuntimeException{
        // 校验结项
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO, "项目不存在");

        String message = "请检查项目积分模块中对项目成员评价和项目评价维护是否完整，变更流程是否审批完成";

        // 需要校验项目评价必填内容是否完成、
        List<ProjectEvaluateDO> evaluateDOList = evaluateMapper.getByProjectId(projectId);
        AssertUtil.checkState(evaluateDOList.stream().noneMatch(e -> ObjectUtil.isNull(e.getScores())),
                message);

        // 纳入积分员工的实际工作量是否录入完成，个人评价是否必填
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.getByProjectId(projectId);
        AssertUtil.checkState(memberEvaluateDOList.stream()
                        .filter(ProjectMemberEvaluateDO::getIncludeStat)
                        .noneMatch(e -> ObjectUtil.isNull(e.getActualWorkload())),
                message);
        AssertUtil.checkState(memberEvaluateDOList.stream().noneMatch(e -> ObjectUtil.isNull(e.getEvaluateGrade())),
                message);

        // 查询该项目的流程
        List<ProjectFlowDO> projectFlowDOList = projectFlowMapper.getByProjectId(projectId);

        // 是否存在审核中的工作流变更、结项流程
        boolean noneWorkloadFlow = projectFlowDOList.stream()
                .filter(e -> FlowTypeEnum.WORKLOAD.getCode().equals(e.getFlowType())
                        || FlowTypeEnum.CONCLUSION.getCode().equals(e.getFlowType()))
                .noneMatch(e -> ForwardFlowStatusEnum.AUDITING.getCode().equals(e.getStatus()));
        AssertUtil.checkState(noneWorkloadFlow, message);

        // 是否存发布延期流程
        List<ProjectNodeFlowDO> nodeFlowDOList = projectNodeFlowMapper.getByProjectId(projectId);
        boolean nonePublishFlow = nodeFlowDOList.stream().noneMatch(e -> ForwardFlowStatusEnum.AUDITING.getCode().equals(e.getStatus()));
        AssertUtil.checkState(nonePublishFlow, message);

        // 计划总工作量
        BigDecimal planWorkloadSum = memberEvaluateDOList.stream()
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 再分配工作量
        BigDecimal actualWorkloadSum = memberEvaluateDOList.stream()
                .map(ProjectMemberEvaluateDO::getActualWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 比较两个工作量是否相等
        if (planWorkloadSum.compareTo(actualWorkloadSum) < 0) {
            BigDecimal diffDay = actualWorkloadSum.subtract(planWorkloadSum).setScale(1, RoundingMode.HALF_UP);
            throw new BaseBizRuntimeException("再分配计划工作量之和大于计划总工作量" + diffDay + "天，请调整");
        }
    }

    /**
     * 直接结项，不走审批
     *
     * @param projectId 项目id
     */
    public void directConclusion(Long projectId, String invalidReason) {
        conclusionPreview(projectId);

        // 更新项目评价
        List<ProjectEvaluateDO> evaluates = evaluateMapper.getByProjectId(projectId);
        for (ProjectEvaluateDO evaluate : evaluates) {
            ProjectEvaluateDO evalDO = new ProjectEvaluateDO()
                    .setProjectId(evaluate.getProjectId())
                    .setEvaluateDimensionId(evaluate.getEvaluateDimensionId())
                    .setReviewerScores(evaluate.getScores())
                    .setReviewerScoresDesc(evaluate.getScoresDesc());
            evaluateMapper.update(evalDO);
        }

        ProjectDO projectDO = projectMapper.get(projectId);

        // 取当前时间为结项时间
        Date conclusionDate = new Date();

        // 计算出新旧状态
        Integer oldStatus = projectDO.getStatus();
        Integer newStatus = StrUtil.isEmpty(invalidReason) ? ProjectStatusEnum.CONCLUSION.getCode() : ProjectStatusEnum.INVALID.getCode();

        // 组装，更新项目数据
        ProjectDO updateDO = new ProjectDO();
        updateDO.setId(projectId);
        updateDO.setStatus(newStatus);
        updateDO.setConclusionDate(conclusionDate);
        updateDO.setNodeStatus(ProjectNodeStatusEnum.CONCLUSION.getCode());
        projectMapper.update(updateDO);

        // 结项后状态、结项日期节点
        projectLogComponent.status(projectId, oldStatus, newStatus);
        projectLogComponent.conclusionDate(projectId, conclusionDate);

        // 如果项目状态为中止，需要执行中止逻辑
        if (ProjectStatusEnum.INVALID.getCode().equals(newStatus)) {
            conclusionAfterInvalid(projectId, invalidReason);
        }
    }

    /**
     * 结项后作废项目
     *
     * @param projectId     项目id
     * @param invalidReason 无效原因
     */
    public void conclusionAfterInvalid(Long projectId, String invalidReason) {
        final Integer status = ProjectStatusEnum.INVALID.getCode();

        // 修改中止原因， 添加中止原因日志
        ProjectDO updateReason = new ProjectDO();
        updateReason.setId(projectId);
        updateReason.setInvalidReason(invalidReason);
        projectMapper.update(updateReason);
        projectLogComponent.addLogWhenContentChange("", invalidReason, projectId, BizChangeLogFieldEnum.TERMINATE_REASON.getText());

        //修改产品需求状态
        productDemandComponent.updateProductDemandStatus(projectId, status);

        // 作废解除关联
        projectProductDemandComponent.update(projectId, null);
        bizLabelComponent.deleteLabel(projectId, BizTypeEnum.PROJECT.getCode());

        // 更新任务状态
        taskComponent.updateStatusAsProjectStatusChange(projectId, status, false);
    }
}
