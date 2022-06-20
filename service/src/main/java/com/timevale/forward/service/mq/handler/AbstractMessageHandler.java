package com.timevale.forward.service.mq.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public abstract class AbstractMessageHandler {

    public abstract void handMessage(String processInstanceId);

}
