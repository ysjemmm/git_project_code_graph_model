package com.timevale.forward.service.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.ProjectMilestoneComponent;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.mq.dto.MilestoneDTO;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
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
    private ProjectMilestoneComponent milestoneComponent;

    // 两个工作日
    private final BigDecimal TWO_WORK_DAY = new BigDecimal(2);
    // 一个的工作日毫秒数
    private final BigDecimal WORK_DAY_SECONDS = new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReturnT<String> execute(String s) throws Exception {
        log.info("[InnerProjectRiskJob]开始执行任务");

        // 当前时间
        Date nowDate = new Date();

        // 需要新增、更新的风险
        List<ProjectRiskDO> insertRiskList = new ArrayList<>();
        List<ProjectRiskDO> updateRiskList = new ArrayList<>();

        // 需要更新项目风险的项目, 待启动、规划中、执行中、收尾中、运营中的内部项目
        List<Integer> statusList = CollUtil.newArrayList(ProjectStatusEnum.WAITING.getCode(), ProjectStatusEnum.PLANING.getCode(),
                ProjectStatusEnum.EXECUTING.getCode(), ProjectStatusEnum.FINISHING.getCode(), ProjectStatusEnum.OPERATING.getCode());
        List<ProjectDO> projectDOs = projectMapper.getByStatus(statusList, ProjectCategoryEnum.INNER_PROJECT.getCode());
        List<Long> projectIds = projectDOs.stream().map(ProjectDO::getId).collect(Collectors.toList());

        log.info("[InnerProjectRiskJob]本次更新相关的项目:{}", projectIds);
        if (CollUtil.isEmpty(projectIds)) {
            return ReturnT.SUCCESS;
        }

        // 待处理的风险
        List<ProjectRiskDO> riskDOs = projectRiskMapper.selectByProjectIdListStatus(projectIds, ProjectRiskStatusEnum.PENDING.getCode());
        Map<String, ProjectRiskDO> riskMap = riskDOs.stream().collect(Collectors.toMap(e -> uniqueKey(e.getProjectId(), e.getMainId(), e.getType(), e.getName()), e -> e, (a,b)->a));

        // 项目相关的里程碑
        List<ProjectMilestone> milestones = milestoneComponent.getValidMilestone(projectIds);
        HashBasedTable<Integer, Long, ProjectMilestone> milestoneTable = milestones.stream()
                .map(e -> ImmutableTable.of(e.getType(), e.getRelationId(), e))
                .collect(HashBasedTable::create, HashBasedTable::putAll, HashBasedTable::putAll);

        try {
            List<MilestoneDTO> milestoneDTOList = overDueRisk(milestones);
            for (MilestoneDTO milestoneDTO : milestoneDTOList) {
                solveRisk(nowDate, milestoneDTO, riskMap, milestoneTable, insertRiskList, updateRiskList);
            }
        } catch (Exception e) {
            log.error("[InnerProjectRiskJob]处理判断逾期风险失败,e: {}", e.getMessage());
        }

        try {
            List<ProjectRiskDO> noEntryRiskList = noEntryRisk(projectDOs, milestones, riskMap);
            insertRiskList.addAll(noEntryRiskList);
        } catch (Exception e) {
            log.error("[InnerProjectRiskJob]判断未录入风险失败, e: {}", e.getMessage());
        }

        // 更新任务和项目
        if (CollUtil.isNotEmpty(insertRiskList)) {
            log.info("[InnerProjectRiskJob]新增风险,size:{}", insertRiskList.size());
            projectRiskMapper.batchInsert(insertRiskList);
        }
        if (CollUtil.isNotEmpty(updateRiskList)) {
            log.info("[InnerProjectRiskJob]更新风险,size:{}", updateRiskList.size());
            updateRiskList.forEach(i -> projectRiskMapper.update(i));
        }

        log.info("[InnerProjectRiskJob]任务执行结束");
        return ReturnT.SUCCESS;
    }

    private List<MilestoneDTO> overDueRisk(List<ProjectMilestone> milestoneList) {
        // 需要处理的里程碑
        List<MilestoneDTO> milestoneDTOList = new ArrayList<>();

        // 里程碑关联的任务
        List<Long> mTaskIds = milestoneList.stream()
                .filter(e -> Objects.equals(MilestoneTypeEnum.TASK.getCode(), e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(mTaskIds)) {
            // 筛选出待执行、进行中的任务
            List<TaskDO> mTaskDOs = taskMapper.getByIdList(mTaskIds);
            mTaskDOs = mTaskDOs.stream()
                    .filter(e -> Objects.equals(TaskStatusEnum.WAITING.getCode(), e.getStatus())
                            || Objects.equals(TaskStatusEnum.PROGRESS.getCode(), e.getStatus()))
                    .collect(Collectors.toList());

            // 转换为里程碑相关数据
            milestoneDTOList = mTaskDOs.stream().map(ProjectMilestoneCopier.INSTANCE::task2dto).collect(Collectors.toList());
        }

        // 里程碑关联的项目
        List<Long> mProjectIds = milestoneList.stream()
                .filter(e -> Objects.equals(MilestoneTypeEnum.PROJECT.getCode(), e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(mProjectIds)) {
            // 排除掉已暂停、已完成的项目
            List<ProjectDO> mProjectDOs = projectMapper.getByIds(mProjectIds);
            mProjectDOs = mProjectDOs.stream()
                    .filter(e -> !Objects.equals(ProjectStatusEnum.SUSPEND.getCode(), e.getStatus())
                            && !Objects.equals(ProjectStatusEnum.COMPLETE.getCode(), e.getStatus()))
                    .collect(Collectors.toList());

            // 转换为里程碑相关数据
            milestoneDTOList.addAll(mProjectDOs.stream().map(ProjectMilestoneCopier.INSTANCE::project2dto).collect(Collectors.toList()));
        }

        log.info("[innerProjectRiskJob.overDueRisk]需要处理的里程碑, size:{}", milestoneDTOList.size());
        return milestoneDTOList;
    }

    private List<ProjectRiskDO> noEntryRisk(List<ProjectDO> projectDOList, List<ProjectMilestone> milestoneList, Map<String, ProjectRiskDO> riskMap) {
        // 里程碑未录入风险
        List<ProjectRiskDO> noEntryRiskList = new ArrayList<>();

        ImmutableMap<Long, ProjectDO> projectDOMap = Maps.uniqueIndex(projectDOList, BaseDO::getId);
        Map<Long, Set<Integer>> milestoneGroup = milestoneList.stream()
                .collect(Collectors.groupingBy(ProjectMilestone::getProjectId, Collectors.collectingAndThen(Collectors.toList(),
                        projectMilestones -> projectMilestones.stream().map(ProjectMilestone::getStage).collect(Collectors.toSet()))));

        Integer riskType = ProjectRiskTypeEnum.MILE_STONE_NONE.getCode();

        milestoneGroup.forEach((projectId, stageSet) -> {
            ProjectDO projectDO = projectDOMap.get(projectId);
            if (projectDO == null) {
                return;
            }

            int preStage = 0;
            boolean previous = true;
            List<Integer> validStages = projectDO.getValidStageList();
            for (Integer validStage : validStages) {
                boolean contains = stageSet.contains(validStage);
                // 如果上一个阶段没有里程碑，当前阶段有里程碑，则是里程碑未录入
                String uniqueKey = uniqueKey(projectId, projectId, riskType, ProjectStageEnum.getTextByCode(preStage));
                if (!previous && contains && !riskMap.containsKey(uniqueKey)) {
                    ProjectRiskDO newRisk = new ProjectRiskDO();
                    newRisk.setSign("");
                    newRisk.setMainId(projectId);
                    newRisk.setProjectId(projectId);
                    newRisk.setName(ProjectStageEnum.getTextByCode(preStage));
                    newRisk.setType(ProjectRiskTypeEnum.MILE_STONE_NONE.getCode());
                    noEntryRiskList.add(newRisk);
                }
                previous = contains;
                preStage = validStage;
            }
        });

        return noEntryRiskList;
    }

    /**
     * 得到逾期天数
     *
     * @return {@link BigDecimal}
     */
    private BigDecimal getOverdueDay(Date planDate, Date nowDate) {
        //  计划结束时间 >= 如果当前时间, 直接跳过
        if (planDate == null || nowDate == null || planDate.compareTo(nowDate) >= 0) {
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
     * @param milestoneTable 里程碑Table
     * @param insertRisks    新增风险列表
     * @param updateRisks    更新风险列表
     */
    private void solveRisk(Date nowDate, MilestoneDTO milestoneDTO,
                           Map<String, ProjectRiskDO> riskMap,
                           HashBasedTable<Integer, Long, ProjectMilestone> milestoneTable,
                           List<ProjectRiskDO> insertRisks, List<ProjectRiskDO> updateRisks) {

        // 不存在里程碑直接返回
        ProjectMilestone  milestone = milestoneTable.get(milestoneDTO.getMilestoneType(), milestoneDTO.getMilestoneRelationId());
        if (milestone == null) {
            return;
        }

        // 判断风险类型
        List<Pair<Integer, BigDecimal>> riskDates = new ArrayList<>();
        if (milestoneDTO.getActualStartDate() == null) {
            BigDecimal overdueDay = getOverdueDay(milestoneDTO.getPlanStartDate(), nowDate);
            if (overdueDay != null) {
                riskDates.add(Pair.of(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), overdueDay));
            }
        }
        if(milestoneDTO.getActualEndDate() == null) {
            BigDecimal overdueDay = getOverdueDay(milestoneDTO.getPlanEndDate(), nowDate);
            if (overdueDay != null) {
                riskDates.add(Pair.of(ProjectRiskTypeEnum.MILE_STONE_END.getCode(), overdueDay));
            }
        }

        // 处理存在风险
        for (Pair<Integer, BigDecimal> riskData : riskDates) {
            Integer riskType = riskData.getKey();
            BigDecimal overdueDay = riskData.getValue();

            // 判断是否已存在风险
            String uniqueKey = uniqueKey(milestone.getProjectId(), milestone.getId(), riskType, milestone.getMilestoneName());
            ProjectRiskDO riskDO = riskMap.get(uniqueKey);
            if (riskDO == null || !riskType.equals(riskDO.getType())) {
                ProjectRiskDO newRisk = new ProjectRiskDO();
                newRisk.setType(riskType);
                newRisk.setMainId(milestone.getId());
                newRisk.setSign(overdueDay.toString());
                newRisk.setName(milestone.getMilestoneName());
                newRisk.setProjectId(milestone.getProjectId());
                insertRisks.add(newRisk);
            } else if (!Objects.equals(riskDO.getSign(), overdueDay.toString())) {
                riskDO.setSign(overdueDay.toString());
                updateRisks.add(riskDO);
            }
        }
    }

    private String uniqueKey(Long projectId, Long mainId, Integer type, String name) {
        return projectId + "-" + mainId + "-" + type + "-" + name;
    }

}
