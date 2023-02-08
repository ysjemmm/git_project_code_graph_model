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
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneListVO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.TaskStatusEnum;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.forward.service.copy.TaskCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Slf4j
@RestService
@RequiredArgsConstructor
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {

    private final ProjectComponent projectComponent;
    private final TaskService taskService;
    private final ElapsedTimeClient elapsedTimeClient;
    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMilestoneMapper milestoneMapper;

    @Override
    public BaseResult<Void> add(ProjectMilestoneAddReq projectMilestoneAddReq) {
        ProjectDO project = projectMapper.get(projectMilestoneAddReq.getProjectId());
        AssertUtil.notNull(project, "您添加的里程碑所属项目不存在，请刷新后重试");
        if (MilestoneTypeEnum.TASK.getCode().equals(projectMilestoneAddReq.getType())) {
            // 任务类型新增，先新增任务
            TaskAddReq req = TaskCopier.INSTANCE.convert(projectMilestoneAddReq);
            Long elapsedTime = elapsedTimeClient.getElapsedTime(projectMilestoneAddReq.getPlanStartDate(),
                    projectMilestoneAddReq.getPlanEndDate());
            req.setPlanUseTime(new BigDecimal(elapsedTime)
                    .divide(new BigDecimal(60 * 60 * 1000), 2, RoundingMode.DOWN));
            Long taskId = taskService.add(req).getData();
            projectMilestoneAddReq.setRelationId(taskId);
        } else if (MilestoneTypeEnum.PROJECT.getCode().equals(projectMilestoneAddReq.getType())) {
            Long relateProjectId = projectMilestoneAddReq.getRelationId();
            ProjectDO relateProject = projectMapper.get(relateProjectId);
            AssertUtil.notNull(relateProject, "您关联的项目不存在，请刷新后重试");
            AssertUtil.checkState(relateProject.getParentId() == null ||
                    relateProject.getParentList().contains(project.getId()),
                    "您关联里程碑的项目已经被其他项目关联");
            AssertUtil.checkState(project.getParentList().contains(relateProject.getId()),
                    "您关联的项目为当前项目父项目，不可关联");

            if (relateProject.getParentId() == null) {
                // 项目无父节点，则添加该项目为子节点
                projectComponent.attachChildProject(project, relateProject);
            }
        } else {
            return BaseResult.success();
        }
        ProjectMilestone entity = ProjectMilestoneCopier.INSTANCE.convert(projectMilestoneAddReq);
        milestoneMapper.insert(entity);
        return BaseResult.success();
    }

    @Override
    public BaseResult<ProjectMilestoneListVO> listMilestones(Long projectId) {
        ProjectDO currentProject = projectMapper.get(projectId);
        AssertUtil.notNull(currentProject, "当前项目不存在或者已经被删除，请刷新后重试");
        ProjectMilestoneListVO res = new ProjectMilestoneListVO();
        List<ProjectMilestoneVO> resList = new ArrayList<>();
        res.setValidStages(currentProject.getValidStageList());
        res.setList(resList);
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
                resList.add(ProjectMilestoneCopier.INSTANCE.convert(projectMilestone, relateProject));
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
                resList.add(ProjectMilestoneCopier.INSTANCE.convert(taskMilestone, task));
            }
        }
        resList.forEach(m -> {
            m.setProjectId(currentProject.getId());
            m.setProjectName(currentProject.getName());
        });
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<Void> deleteMilestone(Long milestoneId) {
        Optional<ProjectMilestone> milestone = Optional.ofNullable(milestoneMapper.selectById(milestoneId));
        milestone.ifPresent(m -> {
            if (Objects.equals(m.getType(), MilestoneTypeEnum.TASK.getCode())) {
                // 任务类未作废则需要先作废任务
                TaskDO task = taskMapper.getById(m.getRelationId());
                if (task != null && !Objects.equals(task.getStatus(), TaskStatusEnum.INVALID.getCode())) {
                    taskService.updateStatus(m.getRelationId(), TaskStatusEnum.INVALID.getCode());
                }
            }
            milestoneMapper.deleteById(m.getId());
        });
        return BaseResult.success();
    }
}
