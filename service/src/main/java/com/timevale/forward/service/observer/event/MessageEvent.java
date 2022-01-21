package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.utils.SpringContextUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationEvent;

/**
 * @author by YangXu
 * @date 2022/01/21 14:53
 */
public abstract class MessageEvent extends ApplicationEvent {
    protected String domainName;

    protected ErpMessageClient erpMessageClient;

    protected final String PARAM = "%s/edit?id=%d&type=check";

    public MessageEvent(Object source) {
        super(source);
        domainName = SpringContextUtil.getProperty("domain_name");
        erpMessageClient = (ErpMessageClient) SpringContextUtil.getBean(ErpMessageClient.class);

        if(StringUtils.isEmpty(domainName)){
            domainName = "http://forward-front-forward-v1.projectk8s.tsign.cn/";
        }
    }

    /**
     * 运行
     *
     */
    public abstract void run();
}
