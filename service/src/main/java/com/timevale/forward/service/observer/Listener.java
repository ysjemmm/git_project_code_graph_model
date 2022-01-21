package com.timevale.forward.service.observer;

import com.timevale.forward.service.observer.event.MessageEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2022/01/21 14:53
 */
@Component
public class Listener {
    @EventListener
    public void listen(MessageEvent event){
        event.run();
    }
}
