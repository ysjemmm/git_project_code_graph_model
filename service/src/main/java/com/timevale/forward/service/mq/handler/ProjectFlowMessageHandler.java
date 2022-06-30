package com.timevale.forward.service.mq.handler;

import com.timevale.forward.service.component.ProjectFlowComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public  class ProjectFlowMessageHandler extends AbstractMessageHandler{

    @Resource
    private ProjectFlowComponent projectFlowComponent;

    @Override
    public void handMessage(String processInstanceId){
        projectFlowComponent.updateFlowInfo(processInstanceId);
    }

}
