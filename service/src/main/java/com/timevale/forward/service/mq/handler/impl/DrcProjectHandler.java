package com.timevale.forward.service.mq.handler.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectMilestoneActionMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.facade.api.result.ProjectMilestoneVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.ProjectMilestoneComponent;
import com.timevale.forward.service.component.ProjectRiskComponent;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.observer.event.OtherProjectPublishMsgEvent;
import com.timevale.forward.service.observer.event.OtnProjectPublishMsgEvent;
import com.timevale.forward.service.observer.event.SrEvalEndMsgEvent;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateFormatConst;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/05/15 18:17
 */
@Slf4j
@LogPoint
@Component
@AllArgsConstructor
public class DrcProjectHandler {
    private final ProjectRiskMapper projectRiskMapper;
    private final ElapsedTimeClient elapsedTimeClient;
    private final ProjectRiskComponent projectRiskComponent;
    private final ProjectMilestoneComponent milestoneComponent;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;
    private final ProjectMilestoneActionMapper milestoneActionMapper;

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
        // 新增、删除无需处理
        if (ObjectUtil.equal(DrcActionEnum.INSERT.toString(), body.getAction())
                ||ObjectUtil.equal(DrcActionEnum.DELETE.toString(), body.getAction())) {
            return;
        }

        ProjectDO projectDO = JSON.parseObject(body.getAfter(), ProjectDO.class);
        Optional.ofNullable(projectDO)
                .map(BaseDO::getId)
                .map(e -> milestoneActionMapper.getOne(projectDO.getId(), MilestoneTypeEnum.PROJECT.getCode()))
                .map(e -> milestoneComponent.getMilestone(e.getMilestoneId()))
                .ifPresent(this::solveRisk);
    }

    /**
     * 解决风险
     *
     * @param milestone 里程碑
     */
    public void solveRisk(ProjectMilestoneVO milestone) {
        if (milestone == null) {
            return;
        }

        // 处理未录入风险
        projectRiskComponent.solveNoEntry(milestone.getProjectId());

        // 关联的待处理风险
        Long milestoneId = milestone.getId();
        List<Integer> types = CollUtil.newArrayList(ProjectRiskTypeEnum.MILE_STONE_START.getCode(),
                ProjectRiskTypeEnum.MILE_STONE_END.getCode());
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByMain(milestoneId, ProjectRiskStatusEnum.PENDING.getCode(), types);
        if (CollUtil.isEmpty(riskDOList)) {
            return;
        }

        Date nowDate = new Date();
        for (ProjectRiskDO riskDO : riskDOList) {
            Integer riskStatus = null;
            BigDecimal overdueDay = null;

            Integer riskType = riskDO.getType();

            // 开始时间未录入
            if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), riskType)) {
                Date planStartDate = milestone.getPlanStartDate();
                Date actualStartDate = milestone.getActualStartDate();
                if (actualStartDate == null) {
                    overdueDay = getOverdueDay(planStartDate, nowDate);
                    if (nowDate.compareTo(planStartDate) <= 0) {
                        riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                    } else {
                        riskStatus = ProjectRiskStatusEnum.PENDING.getCode();
                    }
                } else {
                    overdueDay = getOverdueDay(planStartDate, actualStartDate);
                    riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                }
            } else if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_END.getCode(), riskType)) {
                Date planEndDate = milestone.getPlanEndDate();
                Date actualEndDate = milestone.getActualEndDate();
                if (actualEndDate == null) {
                    overdueDay = getOverdueDay(planEndDate, nowDate);
                    if (nowDate.compareTo(planEndDate) <= 0) {
                        riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                    } else {
                        riskStatus = ProjectRiskStatusEnum.PENDING.getCode();
                    }
                } else {
                    overdueDay = getOverdueDay(planEndDate, actualEndDate);
                    riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                }
            }

            // 处理风险
            if (overdueDay != null) {
                ProjectRiskDO updateRiskDO = new ProjectRiskDO();
                updateRiskDO.setId(riskDO.getId());
                updateRiskDO.setSign(overdueDay.toString());
                updateRiskDO.setStatus(riskStatus);
                projectRiskMapper.update(updateRiskDO);
            }
        }
    }

    /**
     * 得到逾期天数
     *
     * @return {@link BigDecimal}
     */
    private BigDecimal getOverdueDay(Date planDate, Date actualDate) {
        if (ObjectUtil.hasNull(planDate, actualDate)) {
            return null;
        }

        if (actualDate.compareTo(planDate) <= 0) {
            return BigDecimal.ZERO;
        }

        log.info("[DrcRiskListener.getOverdueDay]planDate:{}, actualDate:{}", planDate, actualDate);

        // 计算实际工作日
        Long elapsedTimeStamp = elapsedTimeClient.getElapsedTime(planDate, actualDate);
        BigDecimal elapsedTime = new BigDecimal(elapsedTimeStamp);
        return elapsedTime.divide(BigDecimal.valueOf(DateFormatConst.WORK_DAY_SECONDS), 0, RoundingMode.UP);
    }
}
