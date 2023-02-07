package com.timevale.forward.service.impl;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.client.ProjectMilestoneService;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMilestoneMapper milestoneMapper;

    @Override
    public BaseResult<Void> add(ProjectMilestoneAddReq projectMilestoneAddReq) {
        return null;
    }

    @Override
    public BaseResult<List<ProjectMilestoneVO>> listMilestones(Long projectId) {
        ProjectDO currentProject = projectMapper.get(projectId);
        Assert.notNull(currentProject, "当前项目不存在或者已经被删除，请刷新后重试");
        List<ProjectMilestoneVO> res = new ArrayList<>();
        List<ProjectMilestone> milestones = milestoneMapper.selectByProjectId(projectId);
        if (milestones.isEmpty()) {
            return BaseResult.success(res);
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
                res.add(ProjectMilestoneCopier.INSTANCE.convert(projectMilestone, relateProject));
            }
        }
        List<ProjectMilestone> taskMilestones = milestonesByType.get(MilestoneTypeEnum.TASK.getCode());
        if (!taskMilestones.isEmpty()) {
            List<Long> relationIds = taskMilestones.stream().map(ProjectMilestone::getRelationId)
                    .collect(Collectors.toList());
            List<TaskDO> tasks = taskMapper.getByIdList(relationIds);
            Map<Long, TaskDO> taskById = Maps.uniqueIndex(tasks, TaskDO::getId);
            for (ProjectMilestone taskMilestone : taskMilestones) {
                TaskDO task = taskById.get(taskMilestone.getRelationId());
                if (task == null) {
                    continue;
                }
                res.add(ProjectMilestoneCopier.INSTANCE.convert(taskMilestone, task));
            }
        }
        res.forEach(m -> {
            m.setProjectId(currentProject.getId());
            m.setProjectName(currentProject.getName());
        });
        return BaseResult.success(res);
    }
}
