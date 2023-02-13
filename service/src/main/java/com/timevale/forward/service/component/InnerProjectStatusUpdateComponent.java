package com.timevale.forward.service.component;

import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
    private final ProjectMilestoneComponent projectMilestoneComponent;
    private final ProjectMilestoneMapper projectMilestoneMapper;

    public void updateFromProject(ProjectDO project) {
        ProjectMilestone milestone =
                projectMilestoneMapper.selectByRelation(project.getId(), MilestoneTypeEnum.PROJECT.getCode());
        if (milestone == null) {
            return;
        }
        updateProjectDateAndStatus(milestone.getProjectId());
    }

    public void updateProjectDateAndStatus(Long projectId) {
        ProjectDO project = projectMapper.get(projectId);
        if (!Objects.equals(project.getCategory(), ProjectCategoryEnum.INNER_PROJECT.getCode())) {
            log.info("not inner project, skip update, projectId: {}", projectId);
        }
        if (ProjectStatusEnum.suspendOrTerminated(project.getStatus())) {
            log.info("project is suspend or terminated, projectId: {}", projectId);
            return;
        }
        try {
            List<ProjectMilestoneVO> validMilestones = getValidMilestones(projectId);
            Date actualStartDate = null;
            Date actualEndDate = null;
            Integer status = ProjectStatusEnum.WAITING.getCode();
            if (!validMilestones.isEmpty()) {
                status = validMilestones.stream().max(Comparator.comparing(ProjectMilestoneVO::getStage))
                        .map(m -> ProjectStageEnum.getByCode(m.getStage()).getStatus().getCode())
                        .orElse(ProjectStatusEnum.WAITING.getCode());
                List<Integer> validStageList = project.getValidStageList();
                Integer startStage = validStageList.get(0);
                Integer endStage = validStageList.get(validStageList.size() - 1);
                actualStartDate = validMilestones.stream().filter(m -> Objects.equals(m.getStage(), startStage))
                        .min(Comparator.comparing(ProjectMilestoneVO::getActualStartDate))
                        .map(ProjectMilestoneVO::getActualStartDate).orElse(null);
                actualEndDate = validMilestones.stream()
                        .filter(m -> Objects.equals(m.getStage(), endStage) && m.getActualEndDate() != null)
                        .max(Comparator.comparing(ProjectMilestoneVO::getActualEndDate))
                        .map(ProjectMilestoneVO::getActualEndDate).orElse(null);
            }
            if (Objects.equals(project.getStatus(), status) &&
                    Objects.equals(project.getActualStartDate(), actualStartDate) &&
                    Objects.equals(project.getActualEndDate(), actualEndDate)) {
                log.info("project status and date not updating cause data is identity, projectId: {}, status: {}, actualStartDate: {}, actualEndDate: {}",
                        projectId, status, actualStartDate, actualEndDate);
                return;
            }
            projectMapper.updateStatusAndDate(projectId, status, actualStartDate, actualEndDate);
            updateFromProject(project);
        } catch (Exception e) {
            log.warn("update project date and status error，get milestones throws exception，projectId: {}, message: {}",
                    projectId, e.getMessage());
        }

    }

    public Integer calcProjectStatus(Long projectId) {
        return getValidMilestones(projectId).stream()
                .max(Comparator.comparing(ProjectMilestoneVO::getStage))
                .map(m -> ProjectStageEnum.getByCode(m.getStage()).getStatus().getCode())
                .orElse(ProjectStatusEnum.WAITING.getCode());
    }

    List<ProjectMilestoneVO> getValidMilestones(Long projectId) {
        List<ProjectMilestoneVO> milestones = projectMilestoneComponent.listByProjectId(projectId);
        return milestones.stream()
                // 存在实际开始时间
                .filter(m -> Objects.nonNull(m.getActualStartDate()))
                // 非作废里程碑
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
    }

}
