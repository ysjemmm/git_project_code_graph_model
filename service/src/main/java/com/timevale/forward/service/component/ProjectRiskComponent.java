package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.model.enums.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
    private TaskMapper taskMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectMilestoneMapper milestoneMapper;

    public void solveNoEntry(Long projectId) {
        log.info("[DrcRiskListener.solveNoEntry]处理可能的未录入风险：projectId:{}", projectId);

        // 查询当前项目的全部里程碑
        List<ProjectMilestone> milestoneList = milestoneMapper.selectByProjectId(projectId);

        // 查询里程碑对应的任务及项目，判断是否全部作废
        List<Long> taskIdList = milestoneList.stream()
                .filter(e -> ObjectUtil.equal(MilestoneTypeEnum.TASK.getCode(), e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());
        List<Long> projectIdList = milestoneList.stream()
                .filter(e -> ObjectUtil.equal(MilestoneTypeEnum.PROJECT.getCode(), e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());

        // 过滤作废里程碑
        Set<String> invalidMilestone = new HashSet<>();
        if (CollUtil.isNotEmpty(taskIdList)) {
            List<TaskDO> taskDOList = taskMapper.getByIdList(taskIdList);
            invalidMilestone = taskDOList.stream().filter(e-> ObjectUtil.equal(TaskStatusEnum.INVALID.getCode(), e.getStatus()))
                    .map(e -> MilestoneTypeEnum.TASK.getCode() + "-" + e.getId())
                    .collect(Collectors.toSet());

        }
        if (CollUtil.isNotEmpty(projectIdList)) {
            List<ProjectDO> projectDOList = projectMapper.getByIds(projectIdList);
            invalidMilestone.addAll(projectDOList.stream().filter(e-> ObjectUtil.equal(TaskStatusEnum.INVALID.getCode(), e.getStatus()))
                    .map(e -> MilestoneTypeEnum.PROJECT.getCode() + "-" + e.getId())
                    .collect(Collectors.toSet()));
        }

        // 过滤后按阶段分组
        Set<String> finalInvalidMilestone = invalidMilestone;
        Map<String, List<ProjectMilestone>> milestoneGroup = milestoneList.stream()
                .filter(e -> !finalInvalidMilestone.contains(e.getType() + "-" + e.getRelationId()))
                .collect(Collectors.groupingBy(e -> ProjectStageEnum.getByCode(e.getStage()).getText()));

        // 当前项目里程碑未录入风险
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectId(projectId);
        Map<String, ProjectRiskDO> noEntryRiskMap = riskDOList.stream()
                .filter(e -> ProjectRiskStatusEnum.PENDING.getCode().equals(e.getStatus())
                        && ProjectRiskTypeEnum.MILE_STONE_NONE.getCode().equals(e.getType()))
                .collect(Collectors.toMap(ProjectRiskDO::getName, e -> e, (a, b) -> a));

        // 内部里程碑
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
            if (milestoneGroup.containsKey(name) || !milestoneGroup.containsKey(nextStageEnum.getText())) {
                projectRiskMapper.updateStatus(id, ProjectRiskStatusEnum.COMPLETE.getCode());
            }
        }
    }
}
