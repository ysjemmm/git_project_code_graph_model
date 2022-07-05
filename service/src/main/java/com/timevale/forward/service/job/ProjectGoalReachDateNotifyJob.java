package com.timevale.forward.service.job;

import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.ProjectGoalMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectGoalDO;
import com.timevale.forward.model.enums.ProjectGoalStatusEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * create on 2022/6/27
 */
@Slf4j
@JobHandler(value = "ProjectGoalReachDateNotifyJob")
public class ProjectGoalReachDateNotifyJob extends IJobHandler {

    private static final String NOTIFY_PATTERN = "项目:%s目标达成时间已到期，请及时更新项目目标完成情况。";
    private static final String TITLE = "项目目标到期提醒";

    @Value("${projectGoal.receiver:zhuque}")
    private String reachGoalReceiver;

    @Resource
    private ProjectGoalMapper projectGoalMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ErpMessageClient erpMessageClient;

    @Override
    public ReturnT<String> execute(String s) {
        log.info("开始项目目标达成日期通知任务");
        List<ProjectGoalDO> projectGoals = projectGoalMapper.getByDate(new Date());
        Set<Long> projectIds = projectGoals.stream().map(ProjectGoalDO::getProjectId)
                .collect(Collectors.toSet());
        List<ProjectDO> projects = projectMapper.getByIds(projectIds);
        Map<Long, ProjectDO> projectById = Maps.uniqueIndex(projects, ProjectDO::getId);
        for (ProjectGoalDO projectGoal : projectGoals) {
            ProjectDO project = projectById.get(projectGoal.getProjectId());
            if (project == null || YesOrNoEnum.NO.getCode().equals(project.getIsWithGoal())) {
                // 无项目或者无项目目标，过滤
                continue;
            }
            if (project.getStatus() < 0) {
                // 已作废、暂停项目，过滤
                continue;
            }
            if (!ProjectGoalStatusEnum.IN_PROGRESS.getCode().equals(projectGoal.getStatus())) {
                // 非进行中项目目标不再提醒
                continue;
            }
            log.info("通知项目到期: {}", projectGoal);
            erpMessageClient.sendMarkdownMsg(MarkdownMsg.builder()
                    .title(TITLE)
                    .content(String.format(NOTIFY_PATTERN, project.getName()))
                    .receivers(Collections.singletonList(reachGoalReceiver)).build());
        }
        log.info("完成项目目标达成日期通知任务");
        return ReturnT.SUCCESS;
    }
}
