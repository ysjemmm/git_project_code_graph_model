package com.timevale.forward.service.mq.handler.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectMilestoneActionMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.ProjectMilestoneActionDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.model.enums.MilestoneTypeEnum;
import com.timevale.forward.service.component.InnerProjectStatusUpdateComponent;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/05/15 18:17
 */
@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class DrcTaskHandler {
    private final ProjectMilestoneMapper milestoneMapper;
    private final ProjectRiskComponent projectRiskComponent;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final ProjectMilestoneActionMapper milestoneActionMapper;
    private final InnerProjectStatusUpdateComponent innerProjectStatusUpdateComponent;

    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(()-> riskHandle(body));
        threadPoolTaskExecutor.execute(()-> beActionHandle(body));
    }

    private void riskHandle(DrcMsgBody body) {
        TaskDO taskDO = JSON.parseObject(body.getAfter(), TaskDO.class);
        Optional.ofNullable(taskDO)
                .map(BaseDO::getId)
                .map(e -> milestoneActionMapper.getOne(e, MilestoneTypeEnum.TASK.getCode()))
                .map(ProjectMilestoneActionDO::getMilestoneId)
                .ifPresent(projectRiskComponent::solveRisk);
    }

    private void beActionHandle(DrcMsgBody body) {
        TaskDO taskDO = JSON.parseObject(body.getAfter(), TaskDO.class);
        Optional.ofNullable(taskDO)
                .map(BaseDO::getId)
                .map(e -> milestoneActionMapper.getOne(e, MilestoneTypeEnum.TASK.getCode()))
                .map(ProjectMilestoneActionDO::getMilestoneId)
                .map(milestoneMapper::selectById)
                .map(ProjectMilestone::getProjectId)
                .ifPresent(innerProjectStatusUpdateComponent::updateProjectDateAndStatus);
    }
}
