package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateFormatConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/02/21 15:56
 */
@Slf4j
@Component
public class ProjectRiskComponent {
    @Resource
    private ElapsedTimeClient elapsedTimeClient;
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectRiskComponent projectRiskComponent;
    @Resource
    private ProjectMilestoneComponent milestoneComponent;

    public void solveNoEntry(Long projectId) {
        log.info("[DrcRiskListener.solveNoEntry]处理可能的未录入风险：projectId:{}", projectId);

        // 查询当前项目的里程碑
        List<ProjectMilestoneVO> milestones = milestoneComponent.listByProjectId(projectId);

        // 存在里程碑的阶段
        Set<String> milestoneStageSet = milestones.stream()
                .map(ProjectMilestoneVO::getStage)
                .map(ProjectStageEnum::getTextByCode)
                .collect(Collectors.toSet());

        // 当前项目里程碑未录入风险
        List<ProjectRiskDO> risks = projectRiskMapper.selectByProjectId(projectId);
        Map<String, ProjectRiskDO> noEntryRiskMap = risks.stream()
                .filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus())
                        && ProjectRiskTypeEnum.MILE_STONE_NONE.getCode().equals(e.getType()))
                .collect(Collectors.toMap(ProjectRiskDO::getName, e -> e, (a, b) -> a));

        // 内部里程碑的全部阶段
        List<ProjectStageEnum> stageEnumList = Arrays.stream(ProjectStageEnum.values())
                .filter(e -> ProjectCategoryEnum.INNER_PROJECT.equals(e.getCategory()))
                .collect(Collectors.toList());

        for (int i = 0; i < stageEnumList.size(); i++) {
            ProjectStageEnum stageEnum = stageEnumList.get(i);
            ProjectRiskDO riskDO = noEntryRiskMap.get(stageEnum.getText());
            if (riskDO == null) {
                continue;
            }

            // 当前阶段包含里程碑，或者下一阶段不包含里程碑
            Long id = riskDO.getId();
            String name = riskDO.getName();
            ProjectStageEnum nextStageEnum = stageEnumList.get(i + 1);
            if (milestoneStageSet.contains(name) || !milestoneStageSet.contains(nextStageEnum.getText())) {
                projectRiskMapper.updateStatus(id, ProjectRiskStatusEnum.COMPLETE.getCode());
            }
        }
    }

    /**
     * 解决风险
     *
     * @param milestoneId 里程碑id
     */
    public void solveRisk(Long milestoneId) {
        ProjectMilestoneVO milestone = milestoneComponent.getMilestone(milestoneId);
        if (milestone == null) {
            return;
        }

        // 处理未录入风险
        projectRiskComponent.solveNoEntry(milestone.getProjectId());

        // 关联的待处理风险
        List<Integer> types = CollUtil.newArrayList(ProjectRiskTypeEnum.MILE_STONE_START.getCode(),
                ProjectRiskTypeEnum.MILE_STONE_END.getCode());
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByMain(milestoneId, ProjectRiskStatusEnum.PENDING.getCode(), types);
        if (CollUtil.isEmpty(riskDOList)) {
            return;
        }

        Date nowDate = new Date();
        for (ProjectRiskDO riskDO : riskDOList) {
            Integer riskStatus = null;
            BigDecimal overdueDay = null;

            Integer riskType = riskDO.getType();

            // 开始时间未录入
            if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), riskType)) {
                Date planStartDate = milestone.getPlanStartDate();
                Date actualStartDate = milestone.getActualStartDate();
                if (actualStartDate == null) {
                    overdueDay = getOverdueDay(planStartDate, nowDate);
                    if (nowDate.compareTo(planStartDate) <= 0) {
                        riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                    } else {
                        riskStatus = ProjectRiskStatusEnum.PENDING.getCode();
                    }
                } else {
                    overdueDay = getOverdueDay(planStartDate, actualStartDate);
                    riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                }
            } else if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_END.getCode(), riskType)) {
                Date planEndDate = milestone.getPlanEndDate();
                Date actualEndDate = milestone.getActualEndDate();
                if (actualEndDate == null) {
                    overdueDay = getOverdueDay(planEndDate, nowDate);
                    if (nowDate.compareTo(planEndDate) <= 0) {
                        riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                    } else {
                        riskStatus = ProjectRiskStatusEnum.PENDING.getCode();
                    }
                } else {
                    overdueDay = getOverdueDay(planEndDate, actualEndDate);
                    riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                }
            }

            // 处理风险
            if (overdueDay != null) {
                ProjectRiskDO updateRiskDO = new ProjectRiskDO();
                updateRiskDO.setId(riskDO.getId());
                updateRiskDO.setSign(overdueDay.toString());
                updateRiskDO.setStatus(riskStatus);
                projectRiskMapper.update(updateRiskDO);
            }
        }
    }

    /**
     * 得到逾期天数
     *
     * @return {@link BigDecimal}
     */
    private BigDecimal getOverdueDay(Date planDate, Date actualDate) {
        if (ObjectUtil.hasNull(planDate, actualDate)) {
            return null;
        }

        if (actualDate.compareTo(planDate) <= 0) {
            return BigDecimal.ZERO;
        }

        log.info("[DrcRiskListener.getOverdueDay]planDate:{}, actualDate:{}", planDate, actualDate);

        // 计算实际工作日
        Long elapsedTimeStamp = elapsedTimeClient.getElapsedTime(planDate, actualDate);
        BigDecimal elapsedTime = new BigDecimal(elapsedTimeStamp);
        return elapsedTime.divide(BigDecimal.valueOf(DateFormatConst.WORK_DAY_SECONDS), 0, RoundingMode.UP);
    }
}
