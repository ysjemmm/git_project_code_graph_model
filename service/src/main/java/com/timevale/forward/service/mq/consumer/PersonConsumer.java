package com.timevale.forward.service.mq.consumer;

import com.timevale.forward.service.mq.listener.PersonListener;
import com.timevale.framework.mq.client.Group;
import com.timevale.framework.mq.client.Topic;
import com.timevale.framework.mq.client.consumer.Consumer;
import com.timevale.framework.mq.client.consumer.ConsumerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author by YangXu
 * @date 2023/05/09 16:30
 */
public class PersonConsumer {
    private final String GROUP_NAME = "forward-person-group";
    private final String TOPIC_NAME = "PERSON_STATUS_TOPIC";

    @Resource
    private ConsumerFactory consumerFactory;
    @Resource
    private PersonListener personListener;

    private final AtomicReference<Consumer> consumer = new AtomicReference<>();

    @PostConstruct
    public void startConsumer() throws Exception {
        if (consumer.get() == null) {
            Consumer consumer = consumerFactory.createConsumer(new Group(GROUP_NAME));
            consumer.subscribe(new Topic(TOPIC_NAME));
            consumer.registerListener(personListener);
            consumer.start();
            this.consumer.compareAndSet(null, consumer);
        }
    }

    @PreDestroy
    public void destroy() {
        Consumer c = consumer.get();
        if (c != null) {
            c.shutdown();
            this.consumer.compareAndSet(c, null);
        }
    }
}
