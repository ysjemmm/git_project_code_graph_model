package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.model.enums.DrcActionEnum;
import com.timevale.forward.model.enums.ProjectRiskStatusEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@Component
@AllArgsConstructor
public class DrcProjectMilestoneHandler {
    private final ProjectRiskMapper projectRiskMapper;
    private final ProjectRiskComponent projectRiskComponent;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(()-> milestoneHandle(body));
    }

    private void milestoneHandle(DrcMsgBody body) {
        // 里程碑新增，处理里程碑未录入风险
        ProjectMilestone milestone = JSON.parseObject(body.getAfter(), ProjectMilestone.class);
        log.info("[DrcProjectMilestoneHandler.milestoneHandle]处理里程碑：milestoneId: {}", milestone.getId());

        // 里程碑关联项目的所有风险，过滤已完成和作废的风险
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByProjectId(milestone.getProjectId());
        riskDOList = riskDOList.stream()
                .filter(e -> ObjectUtil.equal(ProjectRiskStatusEnum.PENDING.getCode(), e.getStatus()))
                .collect(Collectors.toList());

        if (Objects.equals(body.getAction(), DrcActionEnum.INSERT.toString())) {
            // 新增里程碑，处理对应未录入的风险
            String stageName = ProjectStageEnum.getTextByCode(milestone.getStage());
            List<Long> riskIdList = riskDOList.stream()
                    .filter(e -> ObjectUtil.equal(e.getName(), stageName))
                    .map(BaseDO::getId)
                    .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(riskIdList)) {
                projectRiskMapper.updateStatuses(riskIdList, ProjectRiskStatusEnum.COMPLETE.getCode());
            }
        } else if (Objects.equals(body.getAction(), DrcActionEnum.UPDATE.toString())) {
            // 里程碑被删除，作废关联的待处理的风险
            if (Objects.equals(milestone.getIsDeleted(), YesOrNoEnum.YES.getCode())) {
                Long milestoneId = milestone.getId();
                List<Long> riskIdList = riskDOList.stream()
                        .filter(e -> ObjectUtil.equal(milestoneId, e.getMainId()))
                        .map(BaseDO::getId)
                        .collect(Collectors.toList());
                if (CollUtil.isNotEmpty(riskIdList)) {
                    projectRiskMapper.updateStatuses(riskIdList, ProjectRiskStatusEnum.INVALID.getCode());
                }
            }
            // 处理未录入风险
            projectRiskComponent.solveNoEntry(milestone.getProjectId());
        }
    }
}
