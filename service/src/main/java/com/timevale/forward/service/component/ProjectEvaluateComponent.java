package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.HistoryRecordMapper;
import com.timevale.forward.dal.dao.ProjectFlowMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMemberEvaluateMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.request.MemberWorkloadFillReq;
import com.timevale.forward.facade.api.request.MemberWorkloadModifyReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.ProjectWorkloadChangeVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProjectMemberEvaluateCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.security.facade.enums.IncentiveMethodEnum;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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

    private final ProjectMapper projectMapper;
    private final PersonComponent personComponent;
    private final HistoryRecordMapper recordMapper;
    private final ProjectFlowMapper projectFlowMapper;
    private final InnerUserPersonClient innerUserPersonClient;
    private final ProjectMemberEvaluateMapper memberEvaluateMapper;

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
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);

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

        // 不存在基线版本，直接返回，无需表单数据
        if (recordMapper.selectLast(projectId) == null) {
            return new ProjectWorkloadChangeVO().setDirectChangeEnable(true);
        }

        // 旧的计划总工作量、积分总工作量
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);
        BigDecimal planWorkloadSumBefore = memberEvaluateDOList.stream()
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pointsWorkloadSumBefore = memberEvaluateDOList.stream()
                .filter(ProjectMemberEvaluateDO::getIncludeStat)
                .map(ProjectMemberEvaluateDO::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 新的总计划工作量、积分总工作量
        List<MemberWorkloadModifyReq> memberWorkloadList = req.getModifyReqList();
        BigDecimal planWorkloadSumAfter = memberWorkloadList.stream()
                .map(MemberWorkloadModifyReq::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pointsWorkloadSumAfter = memberWorkloadList.stream()
                .filter(MemberWorkloadModifyReq::getIncludeStat)
                .map(MemberWorkloadModifyReq::getPlanWorkload)
                .filter(ObjectUtil::isNotNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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

        // 填装表单数据
        ProjectWorkloadChangeVO result = ProjectMemberEvaluateCopier.INSTANCE.do2vo(projectDO);
        return result.setDirectChangeEnable(false)
                .setPlanWorkloadBefore(planWorkloadSumBefore)
                .setPlanWorkloadAfter(planWorkloadSumAfter)
                .setPointWorkloadBefore(pointsWorkloadSumBefore)
                .setPointWorkloadAfter(pointsWorkloadSumAfter)
                .setPlanWorkloadAddSum(planWorkloadAddSum)
                .setPointWorkloadAddSum(pointsWorkloadAddSum)
                .setChangeTypeList(changeTypeList);
    }

    /**
     * 更新成员
     *
     * @param projectId  项目id
     * @param newMembers 新成员
     */
    public void addMember(Long projectId, Collection<PersonAddReq> newMembers) {
        // 当前积分团队成员
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);
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
        List<PersonDO> oldMemberDOList = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        List<PersonAddReq> oldMembers = PersonCopier.INSTANCE.do2req(oldMemberDOList);

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

//        // 新增成员
//        if (CollUtil.isNotEmpty(addMembers)) {
//            List<ProjectMemberEvaluateDO> newEvalMembers = addMembers.stream()
//                    .map(e -> ProjectMemberEvaluateCopier.INSTANCE.person2do(e, projectId))
//                    .collect(Collectors.toList());
//
//            // 新增落库
//            memberEvaluateMapper.batchInsert(newEvalMembers);
//        }
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
        List<BaseInfoResponse> membersInfo =  innerUserPersonClient.batchGetStaffs(memberIds);
        ImmutableMap<String, BaseInfoResponse> memberInfoMap = Maps.uniqueIndex(membersInfo, BaseInfoResponse::getAccount);

        List<ProjectMemberEvaluateDO> addEvalMembers = members.stream()
                .map(e -> {
                    boolean includeStat = false;

                    // 用户激励方式为积分制的时候，才纳入积分统计
                    final String userId = e.getUserId();
                    BaseInfoResponse memberInfo = memberInfoMap.get(userId);
                    if (memberInfo == null) {
                        log.error("[ProjectEvaluateComponent.addMember]未查询到对应员工的信息:{}", userId);
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
}
