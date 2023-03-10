package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.HistoryRecordMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMemberEvaluateMapper;
import com.timevale.forward.dal.entity.HistoryRecordDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMemberEvaluateDO;
import com.timevale.forward.facade.api.request.MemberWorkloadFillReq;
import com.timevale.forward.facade.api.request.MemberWorkloadModifyReq;
import com.timevale.forward.facade.api.result.ProjectWorkloadChangeVO;
import com.timevale.forward.model.enums.WorkloadChangeTypeEnum;
import com.timevale.forward.service.copy.ProjectMemberEvaluateCopier;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author by YangXu
 * @date 2023/03/10 14:27
 */
@Component
@RequiredArgsConstructor
public class ProjectEvaluateComponent {

    private final ProjectMapper projectMapper;
    private final HistoryRecordMapper recordMapper;
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
     * @param userIdColl 需要记录的成员，为空则为全部成员
     */
    public void additionRecord(Long projectId, Collection<String> userIdColl) {
        // 查询最新版本，生成下一个版本号
        BigDecimal lastVersion = Optional.ofNullable(recordMapper.selectLast(projectId))
                .flatMap(e -> Optional.ofNullable(e.getVersion()))
                .orElse(BigDecimal.ZERO);
        BigDecimal newVersion = lastVersion.add(BigDecimal.ONE);

        // 记录的内容, 过滤无需记录的成员
        List<ProjectMemberEvaluateDO> memberEvaluateDOList = memberEvaluateMapper.selectByProjectId(projectId);
        if (CollUtil.isNotEmpty(userIdColl)) {
            memberEvaluateDOList = memberEvaluateDOList.stream()
                    .filter(e -> userIdColl.contains(e.getUserId()))
                    .collect(Collectors.toList());
        }
        String recordContent = JSON.toJSONString(memberEvaluateDOList);

        // 组装落库
        HistoryRecordDO recordDO = new HistoryRecordDO();
        recordDO.setVersion(newVersion);
        recordDO.setProjectId(projectId);
        recordDO.setRecord_content(recordContent);
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
}
