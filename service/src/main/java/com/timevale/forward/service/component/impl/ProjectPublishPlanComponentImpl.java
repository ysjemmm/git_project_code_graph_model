package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectPublishPlanMapper;
import com.timevale.forward.dal.dto.PublishPlanResultDTO;
import com.timevale.forward.dal.entity.ProjectPublishPlanDO;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;
import com.timevale.forward.model.enums.PublishStatusEnum;
import com.timevale.forward.service.component.ProjectPublishPlanComponent;
import com.timevale.forward.service.integration.publish.PublishPlatformClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectPublishPlanComponentImpl implements ProjectPublishPlanComponent {

    @Resource
    private ProjectPublishPlanMapper projectPublishPlanMapper;

    @Resource
    private PublishPlatformClient publishPlatformClient;

    @Override
    public void add(List<Long> list, Long projectId) {
        List<ProjectPublishPlanDO> newProjectProductLineDos = buildDO(list, projectId);
        List<ProjectPublishPlanDO> existPublishPlans = projectPublishPlanMapper.get(projectId);
        List<Long> existPublishPlanIds = existPublishPlans.stream().map(ProjectPublishPlanDO::getPublishPlanId).collect(Collectors.toList());
        log.info("已存在项目-发布计划:existPublishPlanIds={}", existPublishPlanIds);
        List<ProjectPublishPlanDO> needAddProductLines = new ArrayList<>();
        newProjectProductLineDos.forEach((f) -> {
            if (!existPublishPlanIds.contains(f.getPublishPlanId())) {
                needAddProductLines.add(f);
            }
        });
        if (CollectionUtils.isNotEmpty(needAddProductLines)) {
            projectPublishPlanMapper.batchInsert(needAddProductLines);
        }

        List<Long> newPublishPlanIds = newProjectProductLineDos.stream().map(ProjectPublishPlanDO::getPublishPlanId).collect(Collectors.toList());
        existPublishPlans.forEach((p) -> {
            if (!newPublishPlanIds.contains(p.getPublishPlanId())) {
                p.setIsDeleted(true);
                //删除
                projectPublishPlanMapper.update(p);
            }
        });
    }

    @Override
    public void update(Long publishPlanId, Long projectId) {
        ProjectPublishPlanDO projectPublishPlanDO = new ProjectPublishPlanDO();
        projectPublishPlanDO.setPublishPlanId(publishPlanId);
        projectPublishPlanDO.setProjectId(projectId);
        projectPublishPlanDO.setIsDeleted(true);
        projectPublishPlanMapper.update(projectPublishPlanDO);
    }

    @Override
    public boolean anyMatchNotFinished(Long projectId) {
        List<Long> publishPlanIds = projectPublishPlanMapper.get(projectId).stream().map(ProjectPublishPlanDO::getPublishPlanId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(publishPlanIds)) {
            return false;
        }
        PublishPlanQueryList publishPlanQueryList = new PublishPlanQueryList();
        publishPlanQueryList.setId(StringUtils.join(publishPlanIds, ","));
        PublishPlanResultDTO resultDTO = publishPlatformClient.list(publishPlanQueryList);
        return resultDTO.getList().stream().anyMatch(a -> !PublishStatusEnum.FINISHED.name().equals(a.getReleaseStatus()));
    }

    @Override
    public boolean linkPublishPlan(Long projectId) {
        List<Long> publishPlanIds = projectPublishPlanMapper.get(projectId).stream().map(ProjectPublishPlanDO::getPublishPlanId).collect(Collectors.toList());
        return !CollectionUtils.isEmpty(publishPlanIds);
    }

    private List<ProjectPublishPlanDO> buildDO(List<Long> list, Long projectId) {
        return list.stream().map(t -> {
            ProjectPublishPlanDO projectPublishPlanDO = new ProjectPublishPlanDO();
            projectPublishPlanDO.setPublishPlanId(t);
            projectPublishPlanDO.setProjectId(projectId);
            return projectPublishPlanDO;
        }).collect(Collectors.toList());
    }
}
