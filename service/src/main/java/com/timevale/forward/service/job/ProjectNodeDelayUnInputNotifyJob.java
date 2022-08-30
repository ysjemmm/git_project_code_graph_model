package com.timevale.forward.service.job;

import com.timevale.forward.facade.api.client.ProjectRiskService;
import com.timevale.framework.schedulerT.client.annotaion.JobHandler;
import com.timevale.framework.schedulerT.core.biz.model.ReturnT;
import com.timevale.framework.schedulerT.core.handler.IJobHandler;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author xingyun
 * create on 2022/6/27
 */
@Slf4j
@JobHandler(value = "ProjectNodeDelayUnInputNotifyJob")
public class ProjectNodeDelayUnInputNotifyJob extends IJobHandler {

    @Resource
    private ProjectRiskService projectRiskService;

    @Override
    public ReturnT<String> execute(String s) {
        projectRiskService.syncRiskRecord();
        return ReturnT.SUCCESS;
    }

}
