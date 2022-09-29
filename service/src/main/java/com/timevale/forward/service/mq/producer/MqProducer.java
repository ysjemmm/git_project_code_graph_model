package com.timevale.forward.service.mq.producer;

import com.timevale.framework.mq.client.Group;
import com.timevale.framework.mq.client.producer.Msg;
import com.timevale.framework.mq.client.producer.Producer;
import com.timevale.framework.mq.client.producer.ProducerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author jingchun
 * created on 2022/9/9
 */
@Slf4j
@Component
public class MqProducer {

    private final String groupName = "forward-group";

    @Resource
    private ProducerFactory producerFactory;

    private final AtomicReference<Producer> producer = new AtomicReference<>();

    /**
     * 发送消息，异步发送;
     *
     * @param topic topic
     * @param msg   消息内容, 可以使用 json;
     */
    public void asyncSend(String topic, String msg) {
        CompletableFuture.supplyAsync(() -> {
                    log.info("send mq message, topic: {}, content: {}", topic, msg);
                    return getProducer().offer(new Msg(topic, msg.getBytes()));
                })
                .thenAccept(result -> log.info("send success, msgID = {}, origin = {}", result.getMsgId(), result))
                .exceptionally(e -> {
                    log.error("send msg error, msg: {}", e.getMessage(), e);
                    return null;
                });
    }

    private Producer getProducer() {
        if (producer.get() == null) {
            synchronized (this) {
                if (producer.get() == null) {
                    Producer producer = producerFactory.createProducer(new Group(groupName));
                    try {
                        producer.start();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    this.producer.compareAndSet(null, producer);
                }
            }
        }
        return producer.get();
    }

    /**
     * 关闭客户端;
     */
    @PreDestroy
    private void destroy() {
        producer.get().shutdown();
    }

}
