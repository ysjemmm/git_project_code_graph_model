package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.ProjectMilestoneActionVO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.MilestoneActionCopier;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Slf4j
@LogPoint
@Component
public class ProjectMilestoneComponent {
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private BizChangeLogMapper bizChangeLogMapper;
    @Resource
    private ProjectMilestoneMapper milestoneMapper;
    @Resource
    private MilestoneActionComponent milestoneActionComponent;
    @Resource
    private ProjectMilestoneActionMapper milestoneActionMapper;

    public List<ProjectMilestoneVO> listByProjectId(Long projectId) {
        List<ProjectMilestone> milestones = milestoneMapper.selectByProjectId(projectId);
        if (milestones.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取行动
        List<Long> milestoneIds = milestones.stream().map(ProjectMilestone::getId).collect(Collectors.toList());
        List<ProjectMilestoneActionDO> milestoneActions = milestoneActionMapper.getByMains(milestoneIds);
        Map<Long, List<ProjectMilestoneActionDO>> actionGroup =
                milestoneActions.stream().collect(Collectors.groupingBy(ProjectMilestoneActionDO::getMilestoneId));

        Map<Long, TaskDO> taskMap = new HashMap<>();
        List<Long> taskIds = milestoneActions.stream()
                .filter(e -> MilestoneTypeEnum.TASK.getCode().equals(e.getType()))
                .map(ProjectMilestoneActionDO::getRelationId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(taskIds)) {
            List<TaskDO> tasks = taskMapper.getByIdList(taskIds);
            taskMap = Maps.uniqueIndex(tasks, BaseDO::getId);
        }

        Map<Long, ProjectDO> projectMap = new HashMap<>();
        List<Long> projectIds = milestoneActions.stream()
                .filter(e -> MilestoneTypeEnum.PROJECT.getCode().equals(e.getType()))
                .map(ProjectMilestoneActionDO::getRelationId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(projectIds)) {
            List<ProjectDO> projects = projectMapper.getByIds(projectIds);
            projectMap = Maps.uniqueIndex(projects, BaseDO::getId);
        }

        List<ProjectMilestoneVO> milestoneVOs = ProjectMilestoneCopier.INSTANCE.convert(milestones);
        for (ProjectMilestoneVO milestoneVO : milestoneVOs) {
            List<ProjectMilestoneActionDO> actions = actionGroup.get(milestoneVO.getId());
            if (CollUtil.isEmpty(actions)) {
                continue;
            }

            List<TaskDO> tasks = actions.stream()
                    .filter(e -> MilestoneTypeEnum.TASK.getCode().equals(e.getType()))
                    .map(ProjectMilestoneActionDO::getRelationId)
                    .map(taskMap::get)
                    .collect(Collectors.toList());
            List<ProjectDO> projects = actions.stream()
                    .filter(e -> MilestoneTypeEnum.PROJECT.getCode().equals(e.getType()))
                    .map(ProjectMilestoneActionDO::getRelationId)
                    .map(projectMap::get)
                    .collect(Collectors.toList());

            // 项目行动直接转换
            List<ProjectMilestoneActionVO> projectActions = MilestoneActionCopier.INSTANCE.project2vo(projects);
            // 任务行动由于执行人一对多需要特殊处理
            List<ProjectMilestoneActionVO> taskActions = MilestoneActionCopier.INSTANCE.task2vo(tasks);
            for (ProjectMilestoneActionVO taskAction : taskActions) {
                List<PersonDO> executors = personComponent.select(taskAction.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
                taskAction.setPrincipal(executors.stream().map(PersonDO::getUserName).collect(Collectors.joining(",")));
                taskAction.setPrincipalId(executors.stream().map(PersonDO::getUserId).collect(Collectors.joining(",")));
            }

            // 填充数据
            Collection<ProjectMilestoneActionVO> allActions = CollUtil.addAll(projectActions, taskActions);
            milestoneVO.setActions(allActions);
            allActions.stream()
                    .map(ProjectMilestoneActionVO::getActualStartDate)
                    .filter(Objects::nonNull)
                    .min(Date::compareTo)
                    .ifPresent(milestoneVO::setActualStartDate);
            allActions.stream()
                    .map(ProjectMilestoneActionVO::getActualEndDate)
                    .filter(Objects::nonNull)
                    .max(Date::compareTo)
                    .ifPresent(milestoneVO::setActualEndDate);
        }

        return milestoneVOs;
    }

    public void addMilestoneCreateLog(ProjectMilestone entity) {
        addMilestoneLog(entity, ButtonActionEnum.MILESTONE_ADD);
    }

    public void addMilestoneDeleteLog(ProjectMilestone entity) {
        addMilestoneLog(entity, ButtonActionEnum.MILESTONE_DELETE);
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

    public ProjectMilestoneVO getMilestone(Long milestoneId) {
        ProjectMilestone milestone = milestoneMapper.selectById(milestoneId);
        List<ProjectMilestoneActionVO> actions = milestoneActionComponent.getActions(milestoneId);

        ProjectMilestoneVO milestoneVO = ProjectMilestoneCopier.INSTANCE.convert(milestone);
        actions.stream()
                .map(ProjectMilestoneActionVO::getActualStartDate)
                .filter(Objects::nonNull)
                .min(Date::compareTo)
                .ifPresent(milestoneVO::setActualStartDate);
        actions.stream()
                .map(ProjectMilestoneActionVO::getActualEndDate)
                .filter(Objects::nonNull)
                .max(Date::compareTo)
                .ifPresent(milestoneVO::setActualEndDate);
        milestoneVO.setActions(actions);

        return milestoneVO;
    }

    /**
     * 添加里程碑
     *
     * @param taskDO 任务DO
     */
    public void addMilestone(TaskDO taskDO) {
        if (taskDO == null) {
            return;
        }

        // 添加里程碑
        ProjectMilestone milestone = ProjectMilestoneCopier.INSTANCE.task2do(taskDO);
        milestoneMapper.insert(milestone);
        addMilestoneCreateLog(milestone);

        // 添加里程碑行动
        ProjectMilestoneActionDO action = new ProjectMilestoneActionDO();
        action.setRelationId(taskDO.getId());
        action.setMilestoneId(milestone.getId());
        action.setType(MilestoneTypeEnum.TASK.getCode());
        milestoneActionMapper.add(action);
    }
}
