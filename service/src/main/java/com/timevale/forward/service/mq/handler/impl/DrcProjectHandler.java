package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectMilestoneActionMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestoneActionDO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.handler.DrcHandler;
import com.timevale.forward.service.observer.event.OtherProjectPublishMsgEvent;
import com.timevale.forward.service.observer.event.OtnProjectPublishMsgEvent;
import com.timevale.forward.service.observer.event.ProjectInvalidMsgEvent;
import com.timevale.forward.service.observer.event.SrEvalEndMsgEvent;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/05/15 18:17
 */
@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class DrcProjectHandler implements DrcHandler {
    private final PersonComponent personComponent;
    private final ProjectRiskComponent projectRiskComponent;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final ProjectMilestoneActionMapper milestoneActionMapper;

    @Override
    public void handle(DrcMsgBody body) {
        threadPoolTaskExecutor.execute(() -> msgHandle(body));
        threadPoolTaskExecutor.execute(() -> riskHandle(body));
    }

    /**
     * 根据项目状态发送钉钉消息
     *
     * @param drcMsgBody drc消息体
     */
    public void msgHandle(DrcMsgBody drcMsgBody) {
        ProjectDO beforePj = JSON.parseObject(drcMsgBody.getBefore(), ProjectDO.class);
        ProjectDO afterPj = JSON.parseObject(drcMsgBody.getAfter(), ProjectDO.class);

        // 目前只有产研项目需要发送
        if (!ProjectCategoryEnum.PRODUCT_PROJECT.getCode().equals(afterPj.getCategory())) {
            return;
        }

        // 如果项目变更为已发布
        if (!Objects.equals(beforePj.getStatus(), afterPj.getStatus()) &&
                ProjectStatusEnum.RELEASED.getCode().equals(afterPj.getStatus())) {
            if (StrUtil.isNotEmpty(afterPj.getSrId()) && ProjectKindEnum.PBG_OTN.getCode().equals(afterPj.getKind())) {
                new OtnProjectPublishMsgEvent(this, afterPj.getId(), afterPj.getSrId(), afterPj.getName()).send();
            }
            if (StrUtil.isNotEmpty(afterPj.getPmId())) {
                new OtherProjectPublishMsgEvent(this, afterPj.getId(), afterPj.getPmId(), afterPj.getName()).send();
            }
        }

        // 如果项目变更为已中止
        if (!Objects.equals(beforePj.getStatus(), afterPj.getStatus()) &&
                ProjectStatusEnum.INVALID.getCode().equals(afterPj.getStatus())) {
            List<PersonDO> pds = personComponent.select(afterPj.getId(), PersonTypeEnum.PROJECT_PD.getCode());
            List<String> receivers = pds.stream().map(PersonDO::getUserId).collect(Collectors.toList());
            receivers.add(afterPj.getPmId());
            receivers.add(afterPj.getSrId());
            receivers.add(afterPj.getPrincipalId());
            receivers.add(afterPj.getOtnPrincipalId());
            CollUtil.removeEmpty(receivers);
            CollUtil.distinct(receivers);
            new ProjectInvalidMsgEvent(this, afterPj.getId(), afterPj.getName(), receivers).send();
        }

        // 如果SR评价从无到有
        if (beforePj.getSrEvaluateGrade() == null && afterPj.getSrEvaluateGrade() != null) {
            if (StrUtil.isNotEmpty(afterPj.getPmId())) {
                new SrEvalEndMsgEvent(this, afterPj.getId(), afterPj.getPmId(), afterPj.getName()).send();
            }
        }
    }

    /**
     * 内部项目风险处理
     *
     * @param body DRC消息体
     */
    private void riskHandle(DrcMsgBody body) {
        ProjectDO projectDO = JSON.parseObject(body.getAfter(), ProjectDO.class);

        Optional.ofNullable(projectDO)
                .map(BaseDO::getId)
                .map(e -> milestoneActionMapper.getOne(projectDO.getId(), MilestoneTypeEnum.PROJECT.getCode()))
                .map(ProjectMilestoneActionDO::getMilestoneId)
                .ifPresent(projectRiskComponent::solveRisk);
    }
}
