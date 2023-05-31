package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneActionMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TaskService;
import com.timevale.forward.facade.api.request.TaskAddReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneActionVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.copy.MilestoneActionCopier;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/05/25 16:56
 */
@Slf4j
@LogPoint
@Component
public class MilestoneActionComponent {
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private TaskService taskService;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private PersonComponent personComponent;
    @Resource
    private ProjectComponent projectComponent;
    @Resource
    private ProjectMilestoneMapper milestoneMapper;
    @Resource
    private ProjectMilestoneActionMapper milestoneActionMapper;

    /**
     * 添加任务行动
     *
     * @param milestoneId 里程碑id
     * @param taskAddReqs 任务添加请求
     */
    public void addTaskAction(Long milestoneId, Collection<TaskAddReq> taskAddReqs) {
        if (milestoneId == null || CollUtil.isEmpty(taskAddReqs)) {
            return;
        }

        Optional<ProjectDO> projectOpt = Optional.ofNullable(milestoneMapper.selectById(milestoneId))
                .map(ProjectMilestone::getProjectId)
                .map(projectMapper::get);
        AssertUtil.notNull(projectOpt.isPresent(), "您添加的里程碑所属项目不存在，请刷新后重试");

        // 里程碑行动
        List<ProjectMilestoneActionDO> actions = new ArrayList<>();

        for (TaskAddReq task : taskAddReqs) {
            Long taskId = taskService.add(task).getData();
            actions.add(new ProjectMilestoneActionDO(milestoneId, MilestoneTypeEnum.TASK.getCode(), taskId));
        }

        // 更新里程碑关联关系
        if (CollUtil.isNotEmpty(actions)) {
            milestoneActionMapper.batchAdd(actions);
        }
    }

    /**
     * 添加项目行动
     *
     * @param milestoneId 里程碑id
     * @param relateProjectIds 关联项目id
     */
    public void addProjectAction(Long milestoneId, Collection<Long> relateProjectIds) {
        if (milestoneId == null || CollUtil.isEmpty(relateProjectIds)) {
            return;
        }
        ProjectDO project = Optional.ofNullable(milestoneMapper.selectById(milestoneId))
                .map(ProjectMilestone::getProjectId)
                .map(projectMapper::get)
                .orElse(null);
        AssertUtil.notNull(project, "您添加的里程碑所属项目不存在，请刷新后重试");

        // 里程碑行动
        List<ProjectMilestoneActionDO> actions = new ArrayList<>();
        List<ProjectDO> projects = projectMapper.getByIds(relateProjectIds);
        ImmutableMap<Long, ProjectDO> projectMap = Maps.uniqueIndex(projects, ProjectDO::getId);

        for (Long relateProjectId : relateProjectIds) {
            ProjectDO relateProject = projectMap.get(relateProjectId);

            AssertUtil.notNull(relateProject, "您关联的项目不存在，请刷新后重试");
            AssertUtil.checkState(relateProject.getParentId() == null ||
                            relateProject.getParentList().contains(project.getId()),
                    "您关联里程碑的项目已经被其他项目关联");
            AssertUtil.checkState(!project.getParentList().contains(relateProject.getId()),
                    "您关联的项目为当前项目父项目，不可关联");

            // 项目无父节点，则添加该项目为子节点
            if (relateProject.getParentId() == null) {
                projectComponent.attachChildProject(project, relateProject);
            }
            actions.add(new ProjectMilestoneActionDO(milestoneId, MilestoneTypeEnum.PROJECT.getCode(), relateProjectId));
        }

        // 更新里程碑关联关系
        if (CollUtil.isNotEmpty(actions)) {
            milestoneActionMapper.batchAdd(actions);
        }
    }

    public List<ProjectMilestoneActionVO> getActions(Long milestoneId) {
        List<ProjectMilestoneActionDO> actions = milestoneActionMapper.getByMain(milestoneId);

        // 获取行动实体id
        List<Long> taskIds = actions.stream()
                .filter(e -> MilestoneTypeEnum.TASK.getCode().equals(e.getType()))
                .map(ProjectMilestoneActionDO::getRelationId)
                .collect(Collectors.toList());
        List<Long> projectIds = actions.stream()
                .filter(e -> MilestoneTypeEnum.PROJECT.getCode().equals(e.getType()))
                .map(ProjectMilestoneActionDO::getRelationId)
                .collect(Collectors.toList());

        // 非空则查询并转换
        List<ProjectMilestoneActionVO> result = new ArrayList<>();
        if (CollUtil.isNotEmpty(taskIds)) {
            List<TaskDO> tasks = taskMapper.getByIdList(taskIds);
            List<ProjectMilestoneActionVO> taskActions = MilestoneActionCopier.INSTANCE.task2vo(tasks);
            for (ProjectMilestoneActionVO taskAction : taskActions) {
                List<PersonDO> executors = personComponent.select(taskAction.getId(), PersonTypeEnum.TASK_EXECUTOR.getCode());
                taskAction.setPrincipal(executors.stream().map(PersonDO::getUserName).collect(Collectors.joining(",")));
                taskAction.setPrincipalId(executors.stream().map(PersonDO::getUserId).collect(Collectors.joining(",")));
            }
            result.addAll(taskActions);
        }
        if (CollUtil.isNotEmpty(projectIds)) {
            List<ProjectDO> projects = projectMapper.getByIds(projectIds);
            result.addAll(MilestoneActionCopier.INSTANCE.project2vo(projects));
        }

        return result;
    }

}
