package com.timevale.forward.service.mq.listener;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.dao.ProjectMilestoneMapper;
import com.timevale.forward.dal.dao.ProjectRiskMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectMilestone;
import com.timevale.forward.dal.entity.ProjectRiskDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.mq.dto.DrcMsgBody;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.framework.mq.client.consumer.Listener;
import com.timevale.framework.mq.client.consumer.ReceiveResult;
import com.timevale.framework.mq.client.producer.Msg;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author by YangXu
 * @date 2023/02/10 10:02
 */
@Slf4j
@Component
public class DrcRiskListener implements Listener {
    @Resource
    private ElapsedTimeClient elapsedTimeClient;
    @Resource
    private ProjectRiskMapper projectRiskMapper;
    @Resource
    private ProjectMilestoneMapper projectMilestoneMapper;

    // 一个的工作日毫秒数
    private final BigDecimal WORK_DAY_SECONDS = new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND);

    @Override
    public ReceiveResult receive(List<Msg> msgs) {
        for (Msg msg : msgs) {
            String msgId = msg.getMsgId();
            String message = new String(msg.getBody());
            log.info("收到消息, msgId={}, message={}", msgId, message);

            DrcMsgBody body = JSON.parseObject(message, DrcMsgBody.class);
            try {
                log.info("body: {}", JSON.toJSONString(body));

                solveRisk(body);

                log.info("消费完成,{}", body.getGtId());
            } catch (Exception e) {
                log.error("DRC消费失败,消息={},错误信息={}", msg, e);
            }
        }
        return ReceiveResult.success();
    }

    private void solveRisk(DrcMsgBody body) {
        Date planEndDate;
        Date planStartDate;
        Date actualEndDate;
        Date actualStartDate;
        Long milestoneRelationId;
        MilestoneTypeEnum milestoneType;

        // 判断是任务还是项目
        String tableName = body.getTableName();

        // 里程碑新增，处理里程碑未录入风险
        {
            if (Objects.equals(tableName, "project_milestone")) {
                ProjectMilestone milestone = JSON.parseObject(body.getAfter(), ProjectMilestone.class);
                if (Objects.equals(body.getAction(), "UPDATE")) {
                    // 里程碑被删除，风险作废
                    if (Objects.equals(milestone.getIsDeleted(), YesOrNoEnum.YES.getCode())) {
                        // 关联的风险
                        Long milestoneId = milestone.getId();
                        ArrayList<Integer> types = new ArrayList<>();
                        types.add(ProjectRiskTypeEnum.MILE_STONE_START.getCode());
                        types.add(ProjectRiskTypeEnum.MILE_STONE_END.getCode());
                        types.add(ProjectRiskTypeEnum.MILE_STONE_NONE.getCode());
                        projectRiskMapper.updateStatusByMainId(milestoneId, ProjectRiskStatusEnum.INVALID.getCode(), types);

                        // 上一个阶段的未录入风险
                        ProjectStageEnum preByCode = ProjectStageEnum.getPreByCode(milestone.getStage());
                        Long projectId = milestone.getProjectId();
                        List<ProjectMilestone> projectMilestones = projectMilestoneMapper.selectByStage(projectId, preByCode.getCode());
                        for (ProjectMilestone projectMilestone : projectMilestones) {
                            projectRiskMapper.updateStatusByMainId(
                                    projectMilestone.getId(),
                                    ProjectRiskStatusEnum.COMPLETE.getCode(),
                                    Lists.newArrayList(ProjectRiskTypeEnum.MILE_STONE_NONE.getCode()));
                        }
                    }
                } else if (Objects.equals(body.getAction(), "INSERT")) {
                    Long projectId = milestone.getProjectId();
                    // 查询未处理的里程碑为了录入风险
                    List<ProjectRiskDO> risks = projectRiskMapper.selectByProject(projectId,
                            ProjectRiskTypeEnum.MILE_STONE_NONE.getCode(),
                            ProjectRiskStatusEnum.PENDING.getCode());

                    // 判断是否有相同名称的未录入风险，处理风险
                    String stageName = ProjectStageEnum.getTextByCode(milestone.getStage());
                    for (ProjectRiskDO risk : risks) {
                        if (Objects.equals(stageName, risk.getName())) {
                            projectRiskMapper.updateStatus(risk.getId(), ProjectRiskStatusEnum.COMPLETE.getCode());
                        }
                    }
                }
                return;
            }
        }

        // 项目、任务更新，处理里程碑逾期风险
        {
            if (!Objects.equals(body.getAction(), "UPDATE")) {
                return;
            }
            if (Objects.equals(tableName, "task")) {
                TaskDO taskDO = JSON.parseObject(body.getAfter(), TaskDO.class);
                milestoneRelationId = taskDO.getId();
                milestoneType = MilestoneTypeEnum.TASK;
                planEndDate = taskDO.getPlanEndDate();
                planStartDate = taskDO.getPlanStartDate();
                actualEndDate = taskDO.getActualEndDate();
                actualStartDate = taskDO.getActualStartDate();
            } else if (Objects.equals(tableName, "project")) {
                ProjectDO projectDO = JSON.parseObject(body.getAfter(), ProjectDO.class);
                milestoneRelationId = projectDO.getId();
                milestoneType = MilestoneTypeEnum.PROJECT;
                planEndDate = projectDO.getPlanEndDate();
                planStartDate = projectDO.getPlanStartDate();
                actualEndDate = projectDO.getActualEndDate();
                actualStartDate = projectDO.getActualStartDate();
            } else {
                return;
            }

            // 关联的里程碑
            ProjectMilestone milestone =
                    projectMilestoneMapper.selectByRelation(milestoneRelationId, milestoneType.getCode());
            if (milestone == null) {
                return;
            }

            // 关联的待处理风险
            Long milestoneId = milestone.getId();
            ArrayList<Integer> types = new ArrayList<>();
            types.add(ProjectRiskTypeEnum.MILE_STONE_START.getCode());
            types.add(ProjectRiskTypeEnum.MILE_STONE_END.getCode());
            ProjectRiskDO riskDO = projectRiskMapper.selectByMain(milestoneId, ProjectRiskStatusEnum.PENDING.getCode(),types);
            if (riskDO == null) {
                return;
            }

            // 判断风险是否解决
            BigDecimal overdueDay = null;
            Integer riskType = riskDO.getType();
            if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_START.getCode(), riskType) && actualStartDate != null) {
                overdueDay = getOverdueDay(planStartDate, actualStartDate);
            } else if (Objects.equals(ProjectRiskTypeEnum.MILE_STONE_END.getCode(), riskType) && actualEndDate != null) {
                overdueDay = getOverdueDay(planEndDate, actualEndDate);
            }

            // 处理风险
            if (overdueDay != null) {
                ProjectRiskDO updateRiskDO = new ProjectRiskDO();
                updateRiskDO.setId(riskDO.getId());
                updateRiskDO.setSign(overdueDay.toString());
                updateRiskDO.setStatus(ProjectRiskStatusEnum.COMPLETE.getCode());
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
