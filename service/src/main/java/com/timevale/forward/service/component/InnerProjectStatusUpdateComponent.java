package com.timevale.forward.service.component;

import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneActionMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.ProjectMilestoneActionDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.ProjectCategoryEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
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
@Component
public class InnerProjectStatusUpdateComponent {
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectMilestoneMapper projectMilestoneMapper;
    @Resource
    private ProjectMilestoneActionMapper milestoneActionMapper;
    @Resource
    private ProjectMilestoneComponent projectMilestoneComponent;

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
            List<ProjectMilestoneVO> validMilestones = getActualMilestone(projectId);
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
        return getActualMilestone(projectId).stream()
                .max(Comparator.comparing(ProjectMilestoneVO::getStage))
                .map(m -> ProjectStageEnum.getByCode(m.getStage()).getStatus().getCode())
                .orElse(ProjectStatusEnum.WAITING.getCode());
    }

    /**
     * 获取存在项目实际开始时间的里程碑
     *
     * @param projectId 项目id
     * @return {@link List}<{@link ProjectMilestoneVO}>
     */
    private List<ProjectMilestoneVO> getActualMilestone(Long projectId) {
        List<ProjectMilestoneVO> milestones = projectMilestoneComponent.listByProjectId(projectId);
        return milestones.stream().filter(e -> Objects.nonNull(e.getActualStartDate())).collect(Collectors.toList());
    }

}
