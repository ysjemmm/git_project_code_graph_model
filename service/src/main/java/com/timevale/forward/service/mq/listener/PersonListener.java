package com.timevale.forward.service.mq.listener;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.facade.api.client.PersonService;
import com.timevale.forward.service.mq.dto.PersonStatusDTO;
import com.timevale.framework.mq.client.consumer.Listener;
import com.timevale.framework.mq.client.consumer.ReceiveResult;
import com.timevale.framework.mq.client.producer.Msg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonListener implements Listener {
    private final PersonService personService;

    @Override
    public ReceiveResult receive(List<Msg> msgs) {
        for (Msg msg : msgs) {
            String msgId = msg.getMsgId();
            String message = new String(msg.getBody());
            log.info("[PersonListener]收到消息, msgId={}, message={}", msgId, message);
            try {
                PersonStatusDTO personStatusDTO = JSON.parseObject(message, PersonStatusDTO.class);
                if (personStatusDTO.isResign()) {
                    personService.resignNotice(personStatusDTO.getAccount());
                }
                log.info("[PersonListener]消费完成,{}", msgs);
            } catch (Exception e) {
                log.error("[PersonListener]消费失败, topic:{}, msgId:{},e:{}",msg.getTopic(),msgId, e.getMessage());
            }
        }
        return ReceiveResult.success();
    }
}
