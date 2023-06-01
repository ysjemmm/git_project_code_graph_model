package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.facade.api.result.ProjectMilestoneActionVO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.constant.CommonConstant;
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
    private BizChangeLogMapper bizChangeLogMapper;
    @Resource
    private ProjectMilestoneMapper milestoneMapper;
    @Resource
    private MilestoneActionComponent milestoneActionComponent;

    public List<ProjectMilestoneVO> listByProjectId(Long projectId) {
        List<ProjectMilestone> milestones = milestoneMapper.selectByProjectId(projectId);
        if (milestones.isEmpty()) {
            return Collections.emptyList();
        }

        return milestones.parallelStream()
                .map(ProjectMilestone::getId)
                .map(this::getMilestone)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
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

    public Optional<ProjectMilestoneVO> getMilestone(Long milestoneId) {
        ProjectMilestone milestone = milestoneMapper.selectById(milestoneId);
        if (milestone == null) {
            return Optional.empty();
        }
        ProjectMilestoneVO milestoneVO = ProjectMilestoneCopier.INSTANCE.convert(milestone);

        List<ProjectMilestoneActionVO> actions = milestoneActionComponent.getActions(milestoneId);
        milestoneVO.setActions(Collections.emptyList());

        if (CollUtil.isEmpty(actions)) {
            return Optional.of(milestoneVO);
        }

        // 里程碑获取实际时间要过滤掉作废的行动
        List<ProjectMilestoneActionVO> validActions = actions.stream()
                .filter(e -> {
                    if (MilestoneTypeEnum.TASK.getCode().equals(e.getType())) {
                        return !TaskStatusEnum.INVALID.getCode().equals(e.getStatus());
                    } else {
                        return !ProjectStatusEnum.INVALID.getCode().equals(e.getStatus());
                    }
                })
                .collect(Collectors.toList());

        // 里程碑实际开始时间，取非作废的行动里最小的实际开始时间
        validActions.stream()
                .map(ProjectMilestoneActionVO::getActualStartDate)
                .filter(Objects::nonNull)
                .min(Date::compareTo)
                .ifPresent(milestoneVO::setActualStartDate);
        // 里程碑实际结束时间，当非作废行动都存在实际结束时间时取最大
        boolean noneNull = validActions.stream()
                .map(ProjectMilestoneActionVO::getActualEndDate)
                .noneMatch(Objects::isNull);
        if (noneNull) {
            validActions.stream()
                    .map(ProjectMilestoneActionVO::getActualEndDate)
                    .max(Date::compareTo)
                    .ifPresent(milestoneVO::setActualEndDate);
        }

        return Optional.of(milestoneVO);
    }
}
