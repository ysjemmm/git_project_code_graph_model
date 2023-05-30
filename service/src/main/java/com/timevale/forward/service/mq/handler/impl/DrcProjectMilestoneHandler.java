package com.timevale.forward.service.mq.handler.impl;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.model.enums.ProjectRiskTypeEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class DrcProjectMilestoneHandler {
    private final ProjectRiskMapper projectRiskMapper;
    private final ProjectRiskComponent projectRiskComponent;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(()-> milestoneHandle(body));
    }

    private void milestoneHandle(DrcMsgBody body) {
        ProjectMilestone milestone = JSON.parseObject(body.getAfter(), ProjectMilestone.class);
        AssertUtil.notNull(milestone);
        log.info("[DrcProjectMilestoneHandler.milestoneHandle]处理里程碑：milestoneId: {}", milestone.getId());

        // 里程碑被删除，作废关联的待处理的风险
        if (Objects.equals(milestone.getIsDeleted(), YesOrNoEnum.YES.getCode())) {
            // 里程碑开始结束未录入风险
            List<Integer> types = new ArrayList<>();
            types.add(ProjectRiskTypeEnum.MILE_STONE_START.getCode());
            types.add(ProjectRiskTypeEnum.MILE_STONE_END.getCode());
            projectRiskMapper.delByMain(milestone.getProjectId(), milestone.getId(), types);
        }

        // 处理里程碑未录入风险
        projectRiskComponent.solveNoEntry(milestone.getProjectId());
    }
}
