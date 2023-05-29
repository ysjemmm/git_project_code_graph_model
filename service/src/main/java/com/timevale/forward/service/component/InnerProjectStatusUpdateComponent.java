package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneActionMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.ProjectMilestoneActionDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneActionVO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InnerProjectStatusUpdateComponent {
    private final ProjectMapper projectMapper;
    private final ProjectMilestoneMapper projectMilestoneMapper;
    private final ProjectMilestoneActionMapper milestoneActionMapper;
    private final ProjectMilestoneComponent projectMilestoneComponent;

    public void updateFromProject(ProjectDO project) {
        Optional.ofNullable(project)
                .map(BaseDO::getId)
                .map(e -> milestoneActionMapper.getOne(project.getId(), MilestoneTypeEnum.PROJECT.getCode()))
                .map(ProjectMilestoneActionDO::getMilestoneId)
                .map(projectMilestoneMapper::selectById)
                .map(ProjectMilestone::getProjectId)
                .ifPresent(this::updateProjectDateAndStatus);
    }

    public void updateProjectDateAndStatus(Long projectId) {
        ProjectDO project = projectMapper.get(projectId);
        if (!Objects.equals(project.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode())) {
            log.info("not inner project, skip update, projectId: {}", projectId);
            return;
        }
        if (ProjectStatusEnum.suspendOrTerminated(project.getStatus())) {
            log.info("project is suspend or terminated, projectId: {}", projectId);
            return;
        }
        try {
            List<ProjectMilestoneVO> validMilestones = getValidActionMilestones(projectId);
            Date actualStartDate = null;
            Integer status = ProjectStatusEnum.WAITING.getCode();
            if (!validMilestones.isEmpty()) {
                status = validMilestones.stream().max(Comparator.comparing(ProjectMilestoneVO::getStage))
                        .map(m -> ProjectStageEnum.getByCode(m.getStage()).getStatus().getCode())
                        .orElse(ProjectStatusEnum.WAITING.getCode());
                actualStartDate = validMilestones.stream()
                        .min(Comparator.comparing(ProjectMilestoneVO::getActualStartDate))
                        .map(ProjectMilestoneVO::getActualStartDate)
                        .orElse(null);
            }
            // 如果为待启动，并且存在实际开始时间，则为启动中
            if (ProjectStatusEnum.WAITING.getCode().equals(status) && actualStartDate != null) {
                status = ProjectStatusEnum.STARTING.getCode();
            }
            if (Objects.equals(project.getStatus(), status) &&
                    Objects.equals(project.getActualStartDate(), actualStartDate)) {
                log.info("project status and date not updating cause data is identity, projectId: {}, status: {}, actualStartDate: {}",
                        projectId, status, actualStartDate);
                return;
            }
            projectMapper.updateStatusAndStartDate(projectId, status, actualStartDate);
            updateFromProject(project);
        } catch (Exception e) {
            log.warn("update project date and status error，get milestones throws exception，projectId: {}, message: {}",
                    projectId, e.getMessage());
        }

    }

    public Integer calcProjectStatus(Long projectId) {
        return getValidActionMilestones(projectId).stream()
                .max(Comparator.comparing(ProjectMilestoneVO::getStage))
                .map(m -> ProjectStageEnum.getByCode(m.getStage()).getStatus().getCode())
                .orElse(ProjectStatusEnum.WAITING.getCode());
    }

    /**
     * 获取存在有效行动的里程碑
     *
     * @param projectId 项目id
     * @return {@link List}<{@link ProjectMilestoneVO}>
     */
    public List<ProjectMilestoneVO> getValidActionMilestones(Long projectId) {
        List<ProjectMilestoneVO> milestones = projectMilestoneComponent.listByProjectId(projectId);

        List<ProjectMilestoneVO> validMilestones = new ArrayList<>();
        for (ProjectMilestoneVO milestone : milestones) {
            Collection<ProjectMilestoneActionVO> actions = milestone.getActions();
            if (CollUtil.isEmpty(actions)) {
                continue;
            }

            List<ProjectMilestoneActionVO> validActions = actions.stream()
                    .filter(m -> Objects.nonNull(m.getActualStartDate()))
                    .filter(m -> {
                        if (Objects.equals(m.getType(), MilestoneTypeEnum.TASK.getCode())) {
                            return !Objects.equals(m.getStatus(), TaskStatusEnum.INVALID.getCode());
                        }
                        if (Objects.equals(m.getType(), MilestoneTypeEnum.PROJECT.getCode())) {
                            return !Objects.equals(m.getStatus(), ProjectStatusEnum.INVALID.getCode());
                        }
                        return false;
                    })
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(validActions)) {
                milestone.setActions(validActions);
                validMilestones.add(milestone);
            }
        }
        return validMilestones;
    }

}
