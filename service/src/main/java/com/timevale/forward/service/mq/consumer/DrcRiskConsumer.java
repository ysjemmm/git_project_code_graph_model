package com.timevale.forward.service.mq.consumer;

import com.timevale.forward.service.mq.listener.DrcRiskListener;
import com.timevale.framework.mq.client.Group;
import com.timevale.framework.mq.client.Topic;
import com.timevale.framework.mq.client.consumer.Consumer;
import com.timevale.framework.mq.client.consumer.ConsumerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author by YangXu
 * @date 2023/02/10 09:53
 */
@Slf4j
@Component
public class DrcRiskConsumer {

    private final String GROUP_NAME = "forward-drc-risk-group";
    private final String TOPIC_NAME = "DRC_DS_PROJECT_TASK_RISK";

    @Resource
    private ConsumerFactory consumerFactory;
    @Resource
    private DrcRiskListener drcRiskListener;

    private final AtomicReference<Consumer> consumer = new AtomicReference<>();

    @PostConstruct
    public void startConsumer() throws Exception {
        if (consumer.get() == null) {
            Consumer consumer = consumerFactory.createConsumer(new Group(GROUP_NAME));
            consumer.subscribe(new Topic(TOPIC_NAME));
            consumer.registerListener(drcRiskListener);
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
