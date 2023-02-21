package com.timevale.forward.service.job;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.mq.dto.MilestoneDTO;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/02/07 16:08
 */
@Slf4j
@JobHandler(value = "InnerProjectRiskBackJob")
public class InnerProjectRiskBackJob extends IJobHandler {

    @Resource
    private TaskMapper taskMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ElapsedTimeClient elapsedTimeClient;
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectMilestoneMapper milestoneMapper;
    @Resource
    private ProjectRiskComponent projectRiskComponent;
    @Resource
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    // 一个的工作日毫秒数
    private final BigDecimal WORK_DAY_SECONDS = new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> execute(String s) throws Exception {
        log.info("[InnerProjectRiskBackJob]任务执行开始");
        Date nowDate = new Date();

        // 风险
        List<Integer> types = CollUtil.newArrayList(ProjectRiskTypeEnum.MILE_STONE_START.getCode(),
                ProjectRiskTypeEnum.MILE_STONE_END.getCode(),
                ProjectRiskTypeEnum.MILE_STONE_NONE.getCode());
        List<ProjectRiskDO> riskDOs = projectRiskMapper.selectByStatusTypes(ProjectRiskStatusEnum.PENDING.getCode(), types);

        if (CollUtil.isEmpty(riskDOs)) {
            log.info("[InnerProjectRiskBackJob]当前无风险");
            return ReturnT.SUCCESS;
        }

        // 处理未录入风险的项目
        List<Long> noEntryProjectIdList = riskDOs.stream()
                .filter(e -> ProjectRiskTypeEnum.MILE_STONE_NONE.getCode().equals(e.getType()))
                .map(ProjectRiskDO::getProjectId)
                .distinct()
                .collect(Collectors.toList());
        for (Long projectId : noEntryProjectIdList) {
            threadPoolTaskExecutor.execute(() -> projectRiskComponent.solveNoEntry(projectId));
        }

        // 逾期未录入风险的项目
        List<Long> projectIds = riskDOs.stream()
                .filter(e -> !ProjectRiskTypeEnum.MILE_STONE_NONE.getCode().equals(e.getType()))
                .map(ProjectRiskDO::getProjectId)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(projectIds)) {
            log.info("[InnerProjectRiskBackJob]任务执行结束,无逾期未录入风险");
            return ReturnT.SUCCESS;
        }

        // 风险项目的里程碑
        List<ProjectMilestone> milestones = milestoneMapper.selectByProjectIds(projectIds);
        Map<Long, ProjectMilestone> milestoneMap = milestones.stream()
                .collect(Collectors.toMap(ProjectMilestone::getId, e -> e, (a, b) -> a));

        // 里程碑关联的任务
        List<TaskDO> mTaskDOs = new ArrayList<>();
        List<Long> mTaskIds = milestones.stream()
                .filter(e -> Objects.equals(e.getType(), MilestoneTypeEnum.TASK.getCode()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(mTaskIds)) {
            mTaskDOs = taskMapper.getByIdList(mTaskIds);
        }
        ImmutableMap<Long, TaskDO> mTaskDOMap = Maps.uniqueIndex(mTaskDOs, BaseDO::getId);

        // 里程碑关联的项目
        List<ProjectDO> mProjectDOs = new ArrayList<>();
        List<Long> mProjectIds = milestones.stream()
                .filter(e -> Objects.equals(e.getType(), MilestoneTypeEnum.PROJECT.getCode()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(mProjectIds)) {
            mProjectDOs = projectMapper.getByIds(mProjectIds);
        }
        ImmutableMap<Long, ProjectDO> mProjectDOMap = Maps.uniqueIndex(mProjectDOs, BaseDO::getId);

        // 已完成的风险
        List<ProjectRiskDO> completeRisks = new ArrayList<>();
        for (ProjectRiskDO riskDO : riskDOs) {
            Integer riskType = riskDO.getType();
            Long riskMainId = riskDO.getMainId();

            // 查询对应的里程碑，不存在则作废该风险
            ProjectMilestone milestone = milestoneMap.get(riskMainId);
            if (milestone == null) {
                projectRiskMapper.updateStatus(riskDO.getId(),ProjectRiskStatusEnum.INVALID.getCode());
                continue;
            }

            // 获取关联对象的日期数据
            MilestoneDTO milestoneDTO = null;
            if (Objects.equals(MilestoneTypeEnum.TASK.getCode(), milestone.getType())) {
                TaskDO taskDO = mTaskDOMap.get(milestone.getRelationId());
                if (taskDO != null) {
                    milestoneDTO = ProjectMilestoneCopier.INSTANCE.task2dto(taskDO);
                }

            } else {
                ProjectDO projectDO = mProjectDOMap.get(milestone.getRelationId());
                if (projectDO != null) {
                    milestoneDTO = ProjectMilestoneCopier.INSTANCE.project2dto(projectDO);
                }
            }

            // 不存在对应对象，或者状态为暂停或作废，风险同步作废
            if (milestoneDTO == null || milestoneDTO.getInvalid() || milestoneDTO.getSuspend()) {
                projectRiskMapper.updateStatus(riskDO.getId(),ProjectRiskStatusEnum.INVALID.getCode());
                continue;
            }

            // 处理风险
            if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), riskType)) {
                solveRisk(nowDate, milestoneDTO.getPlanStartDate() , milestoneDTO.getActualStartDate(), riskDO, completeRisks);
            } else {
                solveRisk(nowDate, milestoneDTO.getPlanEndDate(), milestoneDTO.getActualEndDate(), riskDO, completeRisks);
            }
        }

        // 数据更新
        completeRisks.forEach(e -> projectRiskMapper.update(e));
        log.info("[InnerProjectRiskBackJob]任务执行结束");
        return ReturnT.SUCCESS;
    }

    /**
     * 得到逾期天数
     *
     * @return {@link BigDecimal}
     */
    private BigDecimal getOverdueDay(Date planDate, Date actualDate) {
        //  实际时间 <= 当前时间, 返回0
        if (actualDate.compareTo(planDate) <= 0) {
            return BigDecimal.ZERO;
        }

        // 计算实际工作日
        Long elapsedTimeStamp = elapsedTimeClient.getElapsedTime(planDate, actualDate);
        BigDecimal elapsedTime = new BigDecimal(elapsedTimeStamp);
        return elapsedTime.divide(WORK_DAY_SECONDS, 0, RoundingMode.UP);
    }


    /**
     * 解决风险
     *
     * @param planDate   计划日期
     * @param actualDate 实际日期
     */
    private void solveRisk(Date nowDate, Date planDate, Date actualDate, ProjectRiskDO riskDO, List<ProjectRiskDO> completeRisks) {
        Integer riskStatus = null;
        BigDecimal overdueDay = null;

        if (actualDate == null) {
            if (nowDate.compareTo(planDate) <= 0) {
                overdueDay = getOverdueDay(planDate, nowDate);
                riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
            }
        } else {
            overdueDay = getOverdueDay(planDate, actualDate);
            riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
        }

        if (overdueDay != null && riskStatus != null) {
            riskDO.setSign(overdueDay.toString());
            riskDO.setStatus(riskStatus);
            completeRisks.add(riskDO);
            log.info("[InnerProjectRiskBackJob.solveRisk]补偿完成风险：riskId:{}", riskDO.getId());
        }
    }

}
