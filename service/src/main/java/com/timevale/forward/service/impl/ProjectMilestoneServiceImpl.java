package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneActionMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.facade.api.client.ProjectMilestoneService;
import com.timevale.forward.facade.api.request.ProjectMilestoneAddReq;
import com.timevale.forward.facade.api.request.ProjectMilestoneModifyReq;
import com.timevale.forward.facade.api.result.ProjectMilestoneListVO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.InnerProjectStatusUpdateComponent;
import com.timevale.forward.service.component.MilestoneActionComponent;
import com.timevale.forward.service.component.ProjectMilestoneComponent;
import com.timevale.forward.service.component.UserComponent;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Slf4j
@RestService
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectMilestoneMapper milestoneMapper;
    @Resource
    private UserComponent userComponent;
    @Resource
    private InnerProjectStatusUpdateComponent innerProjectStatusUpdateComponent;
    @Resource
    private ProjectMilestoneComponent projectMilestoneComponent;
    @Resource
    private ProjectMilestoneActionMapper milestoneActionMapper;
    @Resource
    private MilestoneActionComponent milestoneActionComponent;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Void> add(ProjectMilestoneAddReq projectMilestoneAddReq) {
        ProjectDO project = projectMapper.get(projectMilestoneAddReq.getProjectId());

        AssertUtil.notNull(project, "您添加的里程碑所属项目不存在，请刷新后重试");
        AssertUtil.checkState(!ProjectStatusEnum.getByCode(project.getStatus()).isTerminated(),
                "项目已完成或者作废，无法新增里程碑");
        AssertUtil.checkState(project.getValidStageList().contains(projectMilestoneAddReq.getStage()),
                "新增里程碑选择的阶段不存在或已删除，请检查");

        // 新增里程碑和日志
        ProjectMilestone entity = ProjectMilestoneCopier.INSTANCE.convert(projectMilestoneAddReq);
        milestoneMapper.insert(entity);
        projectMilestoneComponent.addMilestoneCreateLog(entity);

        // 里程关联的任务和项目
        milestoneActionComponent.addTaskAction(entity.getId(), projectMilestoneAddReq.getTasks());
        milestoneActionComponent.addProjectAction(entity.getId(), projectMilestoneAddReq.getRelateProjectIds());

        // 更新当前项目状态
        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(entity.getProjectId());

        return BaseResult.success();
    }

    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Void> modify(ProjectMilestoneModifyReq projectMilestoneModifyReq) {
        ProjectMilestone milestoneDO = milestoneMapper.selectById(projectMilestoneModifyReq.getId());
        AssertUtil.notNull(milestoneDO, "里程碑不存在");

        // 更新里程碑
        milestoneMapper.update(ProjectMilestoneCopier.INSTANCE.req2do(projectMilestoneModifyReq));

        // 增加里程关联的任务和项目
        milestoneActionComponent.addTaskAction(milestoneDO.getId(), projectMilestoneModifyReq.getTasks());
        milestoneActionComponent.addProjectAction(milestoneDO.getId(), projectMilestoneModifyReq.getRelateProjectIds());

        // 更新当前项目状态
        innerProjectStatusUpdateComponent.updateProjectDateAndStatus(milestoneDO.getProjectId());

        return BaseResult.success();
    }

    @Override
    public BaseResult<ProjectMilestoneListVO> listMilestones(Long projectId) {
        ProjectDO currentProject = projectMapper.get(projectId);
        AssertUtil.notNull(currentProject, "当前项目不存在或者已经被删除，请刷新后重试");
        ProjectMilestoneListVO res = new ProjectMilestoneListVO();
        List<ProjectMilestoneVO> resList = projectMilestoneComponent.listByProjectId(projectId);
        res.setPmId(currentProject.getPmId());
        res.setPm(currentProject.getPm());
        res.setValidStages(currentProject.getValidStageList());
        res.setList(resList);
        res.setIsPMO(userComponent.isPmo());

        resList.forEach(m -> {
            m.setProjectId(currentProject.getId());
            m.setProjectName(currentProject.getName());
            m.setActions(milestoneActionComponent.getActions(m.getId()));
        });
        resList.sort(Comparator.comparing(ProjectMilestoneVO::getStage)
               .thenComparing(ProjectMilestoneVO::getPlanStartDate));

        return BaseResult.success(res);
    }

    @Override
    public BaseResult<List<ProjectMilestoneVO>> getRelatedMilestones(Long projectId) {
        ProjectDO project = projectMapper.get(projectId);
        if (project == null) {
            return BaseResult.success();
        }
        List<ProjectDO> projects = projectMapper.selectByParentIdsRegexp("^" + project.getParentIds());
        List<ProjectMilestone> milestones = milestoneMapper.selectByRelations(projects.stream().map(ProjectDO::getId)
                .collect(Collectors.toList()), MilestoneTypeEnum.PROJECT.getCode());

        List<ProjectMilestoneVO> result = ProjectMilestoneCopier.INSTANCE.convert(milestones);
        result.forEach(m -> m.setActions(milestoneActionComponent.getActions(m.getId())));
        return BaseResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Void> deleteMilestone(Long milestoneId) {
        Optional<ProjectMilestone> milestone = Optional.ofNullable(milestoneMapper.selectById(milestoneId));
        milestone.ifPresent(m -> {
            // 删除里程碑
            milestoneMapper.deleteById(m.getId());
            projectMilestoneComponent.addMilestoneDeleteLog(m);

            // 删除和里程碑关联的行动
            milestoneActionMapper.delByMain(m.getId());

            // 更新项目状态
            innerProjectStatusUpdateComponent.updateProjectDateAndStatus(m.getProjectId());
        });
        return BaseResult.success();
    }
}
