package com.timevale.forward.service.mq.handler;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@FunctionalInterface
public interface MqMessageHandler {

    void handMessage(String processInstanceId);

}
