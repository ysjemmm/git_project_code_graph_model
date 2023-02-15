package com.timevale.forward.service.mq.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.copy.ProjectMilestoneCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.mq.dto.MilestoneDTO;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.framework.mq.client.consumer.Listener;
import com.timevale.framework.mq.client.consumer.ReceiveResult;
import com.timevale.framework.mq.client.producer.Msg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/02/10 10:02
 */
@Slf4j
@Component
public class DrcRiskListener implements Listener {
    @Resource
    private TaskMapper taskMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ElapsedTimeClient elapsedTimeClient;
    @Resource
    private ProjectMilestoneMapper projectMilestoneMapper;

    // 一个的工作日毫秒数
    private final BigDecimal WORK_DAY_SECONDS = new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND);

    @Override
    public ReceiveResult receive(List<Msg> msgs) {
        for (Msg msg : msgs) {
            String msgId = msg.getMsgId();
            String message = new String(msg.getBody());
            log.info("[DrcRiskListener]收到消息, msgId={}, message={}", msgId, message);

            DrcMsgBody body = JSON.parseObject(message, DrcMsgBody.class);
            try {
                log.info("[DrcRiskListener]body: {}", JSON.toJSONString(body));

                if (ObjectUtil.equal(body.getTableName(), DrcTableEnum.PROJECT_MILESTONE.getText())) {
                    milestoneHandler(body);
                } else if (ObjectUtil.equal(body.getTableName(), DrcTableEnum.TASK.getText())
                        || ObjectUtil.equal(body.getTableName(), DrcTableEnum.PROJECT.getText())) {
                    solveRisk(body);
                }

                log.info("[DrcRiskListener]消费完成,{}", body.getGtId());
            } catch (Exception e) {
                log.error("[DrcRiskListener]消费失败, topic:{}, msgId:{}, message:{}",msg.getTopic(),msgId, message);
            }
        }
        return ReceiveResult.success();
    }

    private void milestoneHandler(DrcMsgBody body) {
        // 里程碑新增，处理里程碑未录入风险
        ProjectMilestone milestone = JSON.parseObject(body.getAfter(), ProjectMilestone.class);

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
            // 里程碑被删除，风险作废
            if (Objects.equals(milestone.getIsDeleted(), YesOrNoEnum.YES.getCode())) {
                {
                    // 作废关联的待处理的风险
                    Long milestoneId = milestone.getId();
                    List<Long> riskIdList = riskDOList.stream()
                            .filter(e -> ObjectUtil.equal(milestoneId, e.getMainId()))
                            .map(BaseDO::getId)
                            .collect(Collectors.toList());
                    if (CollUtil.isNotEmpty(riskIdList)) {
                        projectRiskMapper.updateStatuses(riskIdList, ProjectRiskStatusEnum.INVALID.getCode());
                    }
                }
                {
                    // 完成上一个阶段的未录入风险
                    ProjectStageEnum preStage = ProjectStageEnum.getPreStage(milestone.getStage());
                    List<Long> riskIdList = riskDOList.stream()
                            .filter(e -> ObjectUtil.equal(e.getName(), preStage.getText()))
                            .map(BaseDO::getId)
                            .collect(Collectors.toList());
                    if (CollUtil.isNotEmpty(riskIdList)) {
                        projectRiskMapper.updateStatuses(riskIdList, ProjectRiskStatusEnum.COMPLETE.getCode());
                    }
                }
            }
        }
    }

    private void solveRisk(DrcMsgBody body) {
        // 新增项目、任务无需处理
        if (ObjectUtil.equal(DrcActionEnum.INSERT.toString(), body.getAction())) {
            return;
        }

        log.info("[DrcRiskListener.solveRisk]处理项目、任务:{}", body.getGtId());

        MilestoneDTO milestoneDTO;

        // 项目、任务更新，处理里程碑逾期风险
        String tableName = body.getTableName();
        if (Objects.equals(tableName, DrcTableEnum.TASK.getText())) {
            TaskDO taskDO = JSON.parseObject(body.getAfter(), TaskDO.class);
            milestoneDTO = ProjectMilestoneCopier.INSTANCE.task2dto(taskDO);
        } else if (Objects.equals(tableName, DrcTableEnum.PROJECT.getText())) {
            ProjectDO projectDO = JSON.parseObject(body.getAfter(), ProjectDO.class);
            milestoneDTO = ProjectMilestoneCopier.INSTANCE.project2dto(projectDO);
        } else {
            return;
        }

        // 关联的里程碑
        ProjectMilestone milestone = projectMilestoneMapper
                .selectByRelation(milestoneDTO.getMilestoneRelationId(), milestoneDTO.getMilestoneType());
        if (milestone == null) {
            return;
        }

        // 关联的待处理风险
        Long milestoneId = milestone.getId();
        List<Integer> types = CollUtil.newArrayList(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), ProjectRiskTypeEnum.MILE_STONE_END.getCode());
        List<ProjectRiskDO> riskDOList = projectRiskMapper.selectByMain(milestoneId, ProjectRiskStatusEnum.PENDING.getCode(), types);
        solveNoEntry(milestone.getProjectId(), milestone.getStage());
        if (CollUtil.isEmpty(riskDOList)) {
            return;
        }

        // 如果里程碑节点暂停或作废，对应风险作废
        if (milestoneDTO.getSuspend() || milestoneDTO.getInvalid()) {
            List<Long> riskIdList = riskDOList.stream().map(BaseDO::getId).collect(Collectors.toList());
            projectRiskMapper.updateStatuses(riskIdList, ProjectRiskStatusEnum.INVALID.getCode());

            if (milestoneDTO.getInvalid()) {
                solveNoEntry(milestone.getProjectId(), milestone.getStage());
            }
            return;
        }

        Date nowDate = new Date();
        for (ProjectRiskDO riskDO : riskDOList) {
            Integer riskStatus = null;
            BigDecimal overdueDay = null;

            Integer riskType = riskDO.getType();

            // 开始时间未录入
            if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), riskType)) {
                Date planStartDate = milestoneDTO.getPlanStartDate();
                Date actualStartDate = milestoneDTO.getActualStartDate();
                if (actualStartDate == null) {
                    overdueDay = getOverdueDay(planStartDate, nowDate);
                    if (BigDecimal.ZERO.compareTo(overdueDay) <= 0) {
                        riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                    } else {
                        riskStatus = ProjectRiskStatusEnum.PENDING.getCode();
                    }
                } else {
                    overdueDay = getOverdueDay(planStartDate, actualStartDate);
                    riskStatus = ProjectRiskStatusEnum.COMPLETE.getCode();
                }
            } else if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_END.getCode(), riskType)) {
                Date planEndDate = milestoneDTO.getPlanEndDate();
                Date actualEndDate = milestoneDTO.getActualEndDate();
                if (actualEndDate == null) {
                    overdueDay = getOverdueDay(planEndDate, nowDate);
                    if (BigDecimal.ZERO.compareTo(overdueDay) <= 0) {
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

    private void solveNoEntry(Long projectId, Integer stage) {
        log.info("[DrcRiskListener.solveNoEntry]处理可能的未录入风险：projectId:{},stage:{}", projectId, stage);

        // 查询当前阶段的全部里程碑
        List<ProjectMilestone> milestoneList = projectMilestoneMapper.selectByStage(projectId, stage);

        // 查询里程碑对应的任务及项目，判断是否全部作废
        List<Long> taskIdList = milestoneList.stream()
                .filter(e -> ObjectUtil.equal(MilestoneTypeEnum.TASK.getCode(), e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());
        List<Long> projectIdList = milestoneList.stream()
                .filter(e -> ObjectUtil.equal(MilestoneTypeEnum.PROJECT.getCode(), e.getType()))
                .map(ProjectMilestone::getRelationId)
                .collect(Collectors.toList());

        // 判断是否全部作废
        boolean valid = false;
        if (CollUtil.isNotEmpty(taskIdList)) {
            List<TaskDO> taskDOList = taskMapper.getByIdList(taskIdList);
            valid = !taskDOList.stream().allMatch(e-> ObjectUtil.equal(TaskStatusEnum.INVALID.getCode(), e.getStatus()));
        }
        if (CollUtil.isNotEmpty(projectIdList)) {
            List<ProjectDO> projectDOList = projectMapper.getByIds(projectIdList);
            valid |= !projectDOList.stream().allMatch(e -> ObjectUtil.equal(ProjectStatusEnum.INVALID.getCode(), e.getStatus()));
        }

        // 如果当前阶段仍然有有效的里程碑，返回
        if (valid) {
            return;
        }

        // 上一个阶段的未录入风险
        ProjectStageEnum preStage = ProjectStageEnum.getPreStage(stage);
        List<ProjectRiskDO> noEntryRiskDOList = projectRiskMapper.selectByName(projectId, preStage.getText());
        List<Long> noEntryRiskIdList = noEntryRiskDOList.stream()
                .filter(e -> ObjectUtil.equal(ProjectRiskStatusEnum.PENDING.getCode(), e.getStatus()))
                .map(ProjectRiskDO::getId)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(noEntryRiskIdList)) {
            projectRiskMapper.updateStatuses(noEntryRiskIdList, ProjectRiskStatusEnum.COMPLETE.getCode());
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

        //  实际时间 <= 当前时间, 返回0
        if (actualDate.compareTo(planDate) <= 0) {
            return BigDecimal.ZERO;
        }
        log.info("[DrcRiskListener.getOverdueDay]planDate:{}, actualDate:{}", planDate, actualDate);

        // 计算实际工作日
        Long elapsedTimeStamp = elapsedTimeClient.getElapsedTime(planDate, actualDate);
        BigDecimal elapsedTime = new BigDecimal(elapsedTimeStamp);
        return elapsedTime.divide(WORK_DAY_SECONDS, 0, RoundingMode.UP);
    }
}
