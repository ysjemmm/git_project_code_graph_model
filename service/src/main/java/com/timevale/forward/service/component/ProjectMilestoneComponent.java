package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
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
    private final BizChangeLogMapper bizChangeLogMapper;

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

    public void updateMilestoneNameAndStage(TaskDO task) {
        ProjectMilestone milestone = milestoneMapper.selectByRelation(task.getId(), MilestoneTypeEnum.TASK.getCode());
        if (milestone == null) {
            return;
        }
        if (Objects.equals(milestone.getStage(), task.getStage()) &&
                Objects.equals(milestone.getMilestoneName(), task.getName())) {
            return;
        }
        milestone.setMilestoneName(task.getName());
        milestone.setStage(task.getStage());
        milestoneMapper.update(milestone);
    }

    public void addMilestoneCreateLog(ProjectMilestone entity) {
        addMilestoneLog(entity, ButtonActionEnum.MILESTONE_ADD);
    }

    public void addMilestoneDeleteLog(ProjectMilestone entity) {
        addMilestoneLog(entity, ButtonActionEnum.MILESTONE_DELETE);
    }

    public void addMilestoneSuspendLog(Long relationId, Integer type) {
        addMilestoneLog(relationId, type, ButtonActionEnum.MILESTONE_SUSPEND);
    }

    public void addMilestoneEnableLog(Long relationId, Integer type) {
        addMilestoneLog(relationId, type, ButtonActionEnum.MILESTONE_ENABLE);
    }

    public void addMilestoneInvalidLog(Long relationId, Integer type) {
        addMilestoneLog(relationId, type, ButtonActionEnum.MILESTONE_INVALID);
    }

    public void addMilestoneDoneLog(Long relationId, Integer type) {
        addMilestoneLog(relationId, type, ButtonActionEnum.MILESTONE_DONE);
    }

    private void addMilestoneLog(Long relationId, Integer type, ButtonActionEnum action) {
        ProjectMilestone entity = milestoneMapper.selectByRelation(relationId, type);
        if (entity == null) {
            return;
        }
        addMilestoneLog(entity, action);
    }

    private void addMilestoneLog(ProjectMilestone entity, ButtonActionEnum action) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        BizChangeLogDO log = new BizChangeLogDO()
                .setMainId(entity.getProjectId())
                .setType(BizChangeLogTypeEnum.PROJECT.getCode())
                .setAction(action.getText())
                .setField(BizChangeLogFieldEnum.PJ_MILESTONE.getText())
                .setNewValue(entity.getMilestoneName());
        log.setCreateManId(userInfo.getId());
        log.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        bizChangeLogMapper.insert(log);
    }

    /**
     * 得到有效里程碑，对应非任务或项目为非废除状态
     *
     * @param projectIdList 项目id列表
     * @return {@link List}<{@link ProjectMilestone}>
     */
    public List<ProjectMilestone> getValidMilestone(List<Long> projectIdList) {
        if (CollUtil.isEmpty(projectIdList)) {
            return new ArrayList<>();
        }

        List<ProjectMilestone> validMilestoneList = new ArrayList<>();
        List<ProjectMilestone> milestoneList = milestoneMapper.selectByProjectIds(projectIdList);

        List<Long> mTaskIdList = milestoneList.stream()
                .filter(e -> MilestoneTypeEnum.TASK.getCode().equals(e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());

        List<Long> mProjectIdList = milestoneList.stream()
                .filter(e -> MilestoneTypeEnum.PROJECT.getCode().equals(e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());

        if (CollUtil.isNotEmpty(mTaskIdList)) {
            List<TaskDO> mTaskDOList = taskMapper.getByIdList(mTaskIdList);
            Set<Long> validTaskSet = mTaskDOList.stream()
                    .filter(e -> !TaskStatusEnum.INVALID.getCode().equals(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toSet());
            validMilestoneList = milestoneList.stream()
                    .filter(e -> MilestoneTypeEnum.TASK.getCode().equals(e.getType())
                            && validTaskSet.contains(e.getRelationId()))
                    .collect(Collectors.toList());
        }

        if (CollUtil.isNotEmpty(mProjectIdList)) {
            List<ProjectDO> mProjectDOList = projectMapper.getByIds(mProjectIdList);
            Set<Long> validProjectSet = mProjectDOList.stream()
                    .filter(e -> !ProjectStatusEnum.INVALID.getCode().equals(e.getStatus()))
                    .map(BaseDO::getId)
                    .collect(Collectors.toSet());
            List<ProjectMilestone> projectMilestones = milestoneList.stream()
                    .filter(e -> MilestoneTypeEnum.PROJECT.getCode().equals(e.getType())
                            && validProjectSet.contains(e.getRelationId()))
                    .collect(Collectors.toList());
            validMilestoneList.addAll(projectMilestones);
        }

        return validMilestoneList;
    }
}
