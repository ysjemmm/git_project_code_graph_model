package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.config.CommonConfig;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.SpringContextUtil;
import org.springframework.context.ApplicationEvent;

/**
 * @author by YangXu
 * @date 2022/01/21 14:53
 */
public abstract class MessageEvent extends ApplicationEvent {
    protected String domainName;

    protected ErpMessageClient erpMessageClient;

    protected CommonConfig config;

    protected final String PARAM = "%s/edit?id=%d&type=check";

    public MessageEvent(Object source) {
        super(source);
        domainName = SpringContextUtil.getProperty("domain_name");
        erpMessageClient = (ErpMessageClient) SpringContextUtil.getBean(ErpMessageClient.class);
        config = (CommonConfig) SpringContextUtil.getBean(CommonConfig.class);
    }

    public void send() {
        MessageEventPublisher publisher = (MessageEventPublisher)SpringContextUtil.getBean(MessageEventPublisher.class);
        publisher.publish(this);
    }

    /**
     * 运行
     *
     */
    public abstract void run();
}
