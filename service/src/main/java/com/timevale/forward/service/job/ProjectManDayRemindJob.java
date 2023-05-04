package com.timevale.forward.service.job;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.service.observer.event.ProjectManDayRemindEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/8/29/029 15:27
 */
@Slf4j
@JobHandler(value = "projectManDayRemindJob")
public class ProjectManDayRemindJob extends IJobHandler {
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private ManDayMapper manDayMapper;
    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        log.info("projectManDayRemindJob start");

        List<ProjectDO> projectDOList = projectMapper.pageAllOngoingProjects();
        if (CollUtil.isEmpty(projectDOList)) {
            return ReturnT.SUCCESS;
        }

        String sunday = DateUtil.getLastSunDay();

        List<Long> alreadyProjectIdList = manDayMapper.listAlreadyCreateProject(projectDOList, sunday);

        for (ProjectDO projectDO : projectDOList) {
            if (alreadyProjectIdList.contains(projectDO.getId())) {
                continue;
            }
            // 通知需求接收人
            messageEventPublisher.publish(new ProjectManDayRemindEvent(
                    this,
                    projectDO.getId(),
                    projectDO.getPmId(),
                    projectDO.getName()
            ));
        }

        log.info("projectManDayRemindJob end");

        return ReturnT.SUCCESS;
    }
}
