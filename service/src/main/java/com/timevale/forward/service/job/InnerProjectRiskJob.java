package com.timevale.forward.service.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.ProjectMilestoneComponent;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
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

        // 需要更新项目风险的项目, 待启动、规划中、执行中、收尾中、运营中的内部项目
        List<Integer> statusList = CollUtil.newArrayList(
                ProjectStatusEnum.WAITING.getCode(),
                ProjectStatusEnum.STARTING.getCode(),
                ProjectStatusEnum.PLANING.getCode(),
                ProjectStatusEnum.EXECUTING.getCode(),
                ProjectStatusEnum.FINISHING.getCode(),
                ProjectStatusEnum.OPERATING.getCode());
        List<ProjectDO> projectDOs = projectMapper.getByStatus(statusList, ProjectCategoryEnum.INNER_PROJECT.getCode());
        List<Long> projectIds = projectDOs.stream().map(ProjectDO::getId).collect(Collectors.toList());

        log.info("[InnerProjectRiskJob]本次更新相关的项目:{}", projectIds);
        if (CollUtil.isEmpty(projectIds)) {
            return ReturnT.SUCCESS;
        }

        // 待处理的风险
        List<ProjectRiskDO> risks =
                projectRiskMapper.selectByProjectIdListStatus(projectIds, ProjectRiskStatusEnum.PENDING.getCode());
        Set<ProjectRiskDO> riskSet = new HashSet<>(risks);

        // 项目相关的里程碑
        List<ProjectMilestoneVO> milestones = projectIds.stream()
                .map(milestoneComponent::listByProjectId)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        // 判断当前以后的
        List<ProjectRiskDO> newRisks = new ArrayList<>();
        try {
            List<ProjectRiskDO> overDueRisks = createOverdueRisk(milestones);
            newRisks.addAll(overDueRisks);
        } catch (Exception e) {
            log.error("[InnerProjectRiskJob]处理判断逾期风险失败,e: {}", e.getMessage());
        }
        try {
            List<ProjectRiskDO> entryRisk = createEntryRisk(milestones);
            newRisks.addAll(entryRisk);
        } catch (Exception e) {
            log.error("[InnerProjectRiskJob]判断未录入风险失败, e: {}", e.getMessage());
        }

        List<ProjectRiskDO> addRisks = newRisks.stream().filter(e -> !riskSet.contains(e)).collect(Collectors.toList());
        List<ProjectRiskDO> modRisks = newRisks.stream().filter(riskSet::contains).collect(Collectors.toList());
        log.info("[InnerProjectRiskJob]新增风险,size:{}", addRisks.size());
        log.info("[InnerProjectRiskJob]更新风险,size:{}", modRisks.size());
        if (CollUtil.isNotEmpty(addRisks)) {
            projectRiskMapper.batchInsert(addRisks);
        }
        if (CollUtil.isNotEmpty(modRisks)) {
            modRisks.forEach(projectRiskMapper::update);
        }

        log.info("[InnerProjectRiskJob]任务执行结束");
        return ReturnT.SUCCESS;
    }

    private List<ProjectRiskDO> createOverdueRisk(List<ProjectMilestoneVO> milestones) {

        Date nowDate = new Date();

        List<ProjectRiskDO> risks = new ArrayList<>();

        for (ProjectMilestoneVO milestone : milestones) {
            // 判断风险类型
            List<Pair<Integer, BigDecimal>> riskDates = new ArrayList<>();
            if (milestone.getActualStartDate() == null) {
                BigDecimal overdueDay = getOverdueDay(milestone.getPlanStartDate(), nowDate);
                if (overdueDay != null) {
                    riskDates.add(Pair.of(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), overdueDay));
                }
            }
            if(milestone.getActualEndDate() == null) {
                BigDecimal overdueDay = getOverdueDay(milestone.getPlanEndDate(), nowDate);
                if (overdueDay != null) {
                    riskDates.add(Pair.of(ProjectRiskTypeEnum.MILE_STONE_END.getCode(), overdueDay));
                }
            }

            // 处理存在风险
            for (Pair<Integer, BigDecimal> riskData : riskDates) {
                ProjectRiskDO risk = new ProjectRiskDO();
                risk.setType(riskData.getKey());
                risk.setMainId(milestone.getId());
                risk.setSign(riskData.getValue().toString());
                risk.setName(milestone.getMilestoneName());
                risk.setProjectId(milestone.getProjectId());

                risks.add(risk);
            }
        }
        return risks;
    }

    private List<ProjectRiskDO> createEntryRisk(List<ProjectMilestoneVO> milestones) {
        List<ProjectRiskDO> result = new ArrayList<>();

        Map<Long, List<ProjectMilestoneVO>> milestoneGroup =
                milestones.stream().collect(Collectors.groupingBy(ProjectMilestoneVO::getProjectId));

        // 内部里程碑的全部阶段
        List<ProjectStageEnum> stageEnums = Arrays.stream(ProjectStageEnum.values())
                .filter(e -> ProjectCategoryEnum.INNER_PROJECT.equals(e.getCategory()))
                .sorted(Comparator.comparing(ProjectStageEnum::getCode))
                .collect(Collectors.toList());

        milestoneGroup.forEach((k,v) -> {
            Set<Integer> validStageSet = v.stream().map(ProjectMilestoneVO::getStage).collect(Collectors.toSet());

            ProjectStageEnum preStage = null;
            for (ProjectStageEnum nowStage : stageEnums) {
                if (preStage != null) {
                    boolean preContain = validStageSet.contains(preStage.getCode());
                    boolean nowContain = validStageSet.contains(nowStage.getCode());

                    // 如果上一个阶段没有里程碑，当前阶段有里程碑，则是里程碑未录入
                    if (!preContain && nowContain) {
                        ProjectRiskDO newRisk = new ProjectRiskDO();
                        newRisk.setSign("");
                        newRisk.setMainId(k);
                        newRisk.setProjectId(k);
                        newRisk.setName(preStage.getText());
                        newRisk.setType(ProjectRiskTypeEnum.MILE_STONE_NONE.getCode());
                        result.add(newRisk);
                    }
                }
                preStage = nowStage;
            }
        });

        return result;
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

}
