package com.timevale.forward.service.component;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Component
@RequiredArgsConstructor
public class ProjectMilestoneComponent {

    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final PersonMapper personMapper;

    public List<ProjectMilestoneVO> listByProjectId(Long projectId) {
        List<ProjectMilestoneVO> resList = new ArrayList<>();

        List<ProjectMilestone> milestones = milestoneMapper.selectByProjectId(projectId);
        if (milestones.isEmpty()) {
            return resList;
        }
        ListMultimap<Integer, ProjectMilestone> milestonesByType =
                Multimaps.index(milestones, ProjectMilestone::getType);
        List<ProjectMilestone> projectMilestones = milestonesByType.get(MilestoneTypeEnum.PROJECT.getCode());
        if (!projectMilestones.isEmpty()) {
            List<Long> relationIds = projectMilestones.stream().map(ProjectMilestone::getRelationId)
                    .collect(Collectors.toList());
            List<ProjectDO> projects = projectMapper.getByIds(relationIds);
            Map<Long, ProjectDO> projectById = Maps.uniqueIndex(projects, ProjectDO::getId);
            for (ProjectMilestone projectMilestone : projectMilestones) {
                ProjectDO relateProject = projectById.get(projectMilestone.getRelationId());
                if (relateProject == null) {
                    continue;
                }
                resList.add(ProjectMilestoneCopier.INSTANCE.convert(projectMilestone, relateProject));
            }
        }
        List<ProjectMilestone> taskMilestones = milestonesByType.get(MilestoneTypeEnum.TASK.getCode());
        if (!taskMilestones.isEmpty()) {
            List<Long> relationIds = taskMilestones.stream().map(ProjectMilestone::getRelationId)
                    .collect(Collectors.toList());
            List<TaskDO> tasks = taskMapper.getByIdList(relationIds);
            //1.填充人员信息
            Map<Long, TaskDO> taskById = Maps.uniqueIndex(tasks, TaskDO::getId);
            Map<Long, List<PersonDO>> executorMap = personMapper.get(taskById.keySet(),
                            PersonTypeEnum.TASK_EXECUTOR.getCode())
                    .stream().collect(Collectors.groupingBy(PersonDO::getMainId));
            for (ProjectMilestone taskMilestone : taskMilestones) {
                TaskDO task = taskById.get(taskMilestone.getRelationId());
                if (task == null) {
                    continue;
                }
                resList.add(ProjectMilestoneCopier.INSTANCE.convert(taskMilestone, task, executorMap.get(task.getId())));
            }
        }
        return resList;
    }

}
