package com.timevale.forward.service.observer.publisher;

import com.timevale.forward.service.observer.event.MessageEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author by YangXu
 * @date 2022/01/21 14:56
 */
@Component
public class MessageEventPublisher {
    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    public void publish(MessageEvent event){
        applicationEventPublisher.publishEvent(event);
    }

}
