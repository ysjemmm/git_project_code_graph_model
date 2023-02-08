package com.timevale.forward.service.job;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;

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
@JobHandler(value = "InnerProjectRiskJob")
public class InnerProjectRiskJob extends IJobHandler {

    @Resource
    private TaskMapper taskMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ElapsedTimeClient elapsedTimeClient;
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectMilestoneMapper projectMilestoneMapper;
    @Resource
    private TransactionTemplate transactionTemplate;

    // 两个工作日
    private final BigDecimal TWO_WORK_DAY = new BigDecimal(2);
    // 一个的工作日毫秒数
    private final BigDecimal WORK_DAY_SECONDS = new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND);

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        // 当前时间
        Date nowDate = new Date();

        // 需要新增、更新的风险
        List<ProjectRiskDO> insertRisks = new ArrayList<>();
        List<ProjectRiskDO> updateRisks = new ArrayList<>();

        // 需要更新项目风险的项目, 待启动、规划中、执行中、收尾中、运营中的项目
        List<Integer> statusList = new ArrayList<>();
        statusList.add(ProjectStatusEnum.WAITING.getCode());
        statusList.add(ProjectStatusEnum.PLANING.getCode());
        statusList.add(ProjectStatusEnum.EXECUTING.getCode());
        statusList.add(ProjectStatusEnum.FINISHING.getCode());
        statusList.add(ProjectStatusEnum.OPERATING.getCode());
        List<ProjectDO> projectDOList = projectMapper.getByStatus(statusList, ProjectCategoryEnum.INNER_PROJECT.getCode());
        List<Long> projectIds = projectDOList.stream().map(ProjectDO::getId).collect(Collectors.toList());

        // 待处理的风险
        List<ProjectRiskDO> riskDOs = projectRiskMapper.selectByProjectIdListStatus(projectIds, ProjectRiskStatusEnum.PENDING.getCode());
        ImmutableMap<Long, ProjectRiskDO> riskMap = Maps.uniqueIndex(riskDOs, ProjectRiskDO::getMainId);

        // 项目相关的里程碑
        List<ProjectMilestone> milestones = projectMilestoneMapper.selectByProjectIds(projectIds);
        HashBasedTable<Integer, Long, ProjectMilestone> milestoneTable = milestones.stream()
                .map(e -> ImmutableTable.of(e.getType(), e.getRelationId(), e))
                .collect(HashBasedTable::create, HashBasedTable::putAll, HashBasedTable::putAll);

        {
            // 里程碑关联的任务
            List<Long> mTaskIds = milestones.stream()
                    .filter(e -> Objects.equals(MilestoneTypeEnum.TASK.getCode(), e.getType()))
                    .map(ProjectMilestone::getRelationId)
                    .collect(Collectors.toList());
            // 筛选出待执行、进行中的任务
            List<TaskDO> mTaskDOs = taskMapper.getByIdList(mTaskIds);
            mTaskDOs = mTaskDOs.stream()
                    .filter(e -> Objects.equals(TaskStatusEnum.WAITING.getCode(), e.getStatus())
                            || Objects.equals(TaskStatusEnum.PROGRESS.getCode(), e.getStatus()))
                    .collect(Collectors.toList());

            for (TaskDO mTaskDO : mTaskDOs) {
                solveRisk(nowDate, mTaskDO, riskMap, milestoneTable, insertRisks, updateRisks);
            }
        }

        {
            // 里程碑关联的项目
            List<Long> mProjectIds = milestones.stream()
                    .filter(e -> Objects.equals(MilestoneTypeEnum.PROJECT.getCode(), e.getType()))
                    .map(ProjectMilestone::getRelationId)
                    .collect(Collectors.toList());
            List<ProjectDO> mProjectDOs = projectMapper.getByIds(mProjectIds);
            // 排除掉已暂停、已完成的项目
            mProjectDOs = mProjectDOs.stream()
                    .filter(e -> !Objects.equals(ProjectStatusEnum.SUSPEND.getCode(), e.getStatus())
                            && !Objects.equals(ProjectStatusEnum.COMPLETE.getCode(), e.getStatus()))
                    .collect(Collectors.toList());

            for (ProjectDO mProjectDO : mProjectDOs) {
                solveRisk(nowDate, mProjectDO, riskMap, milestoneTable, insertRisks, updateRisks);
            }
        }

        // 更新任务和项目
        transactionTemplate.execute(e -> {
            try {
                if (CollUtil.isNotEmpty(insertRisks)) {
                    projectRiskMapper.batchInsert(insertRisks);
                }
                if (CollUtil.isNotEmpty(updateRisks)) {
                    updateRisks.forEach(i -> projectRiskMapper.update(i));
                }
            } catch (Exception exception) {
                TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            }
            return true;
        });


        return ReturnT.SUCCESS;
    }

    /**
     * 得到逾期天数
     *
     * @return {@link BigDecimal}
     */
    private BigDecimal getOverdueDay(Date planDate, Date nowDate) {
        //  计划结束时间 >= 如果当前时间, 直接跳过
        if (planDate.compareTo(nowDate) >= 0) {
            return null;
        }

        // 计算实际工作日
        Long elapsedTimeStamp = elapsedTimeClient.getElapsedTime(planDate, nowDate);
        BigDecimal elapsedTime = new BigDecimal(elapsedTimeStamp);
        BigDecimal elapsedDay = elapsedTime.divide(WORK_DAY_SECONDS, 0, RoundingMode.DOWN);

        // 如果大于两个工作日，逾期
        if (TWO_WORK_DAY.compareTo(elapsedDay) < 0) {
            return elapsedDay;
        }
        return null;
    }

    /**
     * 解决风险
     *
     * @param nowDate        当前时间日期
     * @param o              数据
     * @param riskMap        风险Map
     * @param milestoneTable 里程碑Table
     * @param insertRisks    新增风险列表
     * @param updateRisks    更新风险列表
     */
    private void solveRisk(Date nowDate, Object o, ImmutableMap<Long, ProjectRiskDO> riskMap,
                           HashBasedTable<Integer, Long, ProjectMilestone> milestoneTable,
                           List<ProjectRiskDO> insertRisks, List<ProjectRiskDO> updateRisks) {
        Date planEndDate;
        Date planStartDate;
        Date actualStartDate;
        BigDecimal overdueDay;
        ProjectMilestone milestone;
        ProjectRiskTypeEnum riskTypeEnum;

        // 区分获取数据
        if (o instanceof TaskDO) {
            actualStartDate = ((TaskDO) o).getActualStartDate();
            planStartDate = ((TaskDO) o).getPlanStartDate();
            planEndDate = ((TaskDO) o).getPlanEndDate();

            milestone = milestoneTable.get(MilestoneTypeEnum.TASK.getCode(), ((TaskDO) o).getId());
        } else {
            actualStartDate = ((ProjectDO) o).getActualStartDate();
            planStartDate = ((ProjectDO) o).getPlanStartDate();
            planEndDate = ((ProjectDO) o).getPlanEndDate();

            milestone = milestoneTable.get(MilestoneTypeEnum.PROJECT.getCode(), ((ProjectDO) o).getId());
        }

        // 判断风险类型
        if (actualStartDate == null) {
            overdueDay = getOverdueDay(planStartDate, nowDate);
            riskTypeEnum = ProjectRiskTypeEnum.MILE_STONE_START;
        } else {
            overdueDay = getOverdueDay(planEndDate, nowDate);
            riskTypeEnum = ProjectRiskTypeEnum.MILE_STONE_END;
        }

        // 如果逾期不超过两个工作日，直接跳过
        if (overdueDay == null) {
           return;
        }

        // 判断是否已存在风险
        if (milestone != null) {
            ProjectRiskDO riskDO = riskMap.get(milestone.getId());
            if (riskDO == null) {
                ProjectRiskDO newRisk = new ProjectRiskDO();
                newRisk.setMainId(milestone.getId());
                newRisk.setSign(overdueDay.toString());
                newRisk.setType(riskTypeEnum.getCode());
                newRisk.setName(milestone.getMilestoneName());
                newRisk.setProjectId(milestone.getProjectId());
                insertRisks.add(newRisk);
            } else if (!Objects.equals(riskDO.getSign(), overdueDay.toString())) {
                riskDO.setSign(overdueDay.toString());
                updateRisks.add(riskDO);
            }
        }
    }

}
