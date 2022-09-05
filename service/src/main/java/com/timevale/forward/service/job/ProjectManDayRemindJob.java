package com.timevale.forward.service.job;

import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.observer.event.BizDemandToReceiveMsgEvent;
import com.timevale.forward.service.observer.event.ProjectManDayRemindEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.PageUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import com.timevale.mandarin.common.query.QueryBase;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author xiaoyun
 * @date 2022/8/29/029 15:27
 */
@Slf4j
@JobHandler(value = "projectManDayRemindJob")
public class ProjectManDayRemindJob extends IJobHandler {
    @Resource
    private ProjectComponent projectComponent;
    @Resource
    private ManDayMapper manDayMapper;
    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Override
    public ReturnT<String> execute(String s) throws Exception {
        log.info("projectManDayRemindJob start");

        PageUtil.page(this::page);

        log.info("projectManDayRemindJob end");

        return null;
    }

    private List page(QueryBase queryBase) {
        List<ProjectDO> projectList = projectComponent.pageAllOngoingProjects(queryBase);
        if (CollectionUtils.isEmpty(projectList)) {
            return projectList;
        }

        String sunday = DateUtil.getLastSunDay();

        List<Long> alreadyProjectIdList = manDayMapper.listAlreadyCreateProject(projectList, sunday);

        for (ProjectDO projectDO : projectList) {
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

        return projectList;
    }
}
