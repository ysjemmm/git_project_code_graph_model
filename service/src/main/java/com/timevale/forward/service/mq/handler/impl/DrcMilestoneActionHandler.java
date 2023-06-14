package com.timevale.forward.service.mq.handler.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.entity.ProjectMilestoneActionDO;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.handler.DrcHandler;
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
public class DrcMilestoneActionHandler  implements DrcHandler {
    private final ProjectRiskComponent projectRiskComponent;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Override
    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(()-> riskHandle(body));
    }

    private void riskHandle(DrcMsgBody body) {
        ProjectMilestoneActionDO actionDO = JSON.parseObject(body.getAfter(), ProjectMilestoneActionDO.class);

        Optional.ofNullable(actionDO)
                .map(ProjectMilestoneActionDO::getMilestoneId)
                .ifPresent(projectRiskComponent::solveRisk);
    }
}
