package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by xingyun
 * @date 2022/11/22 11:20
 */
public class ProductDemandToCopiedMsgEvent extends MessageEvent {

    private final Long productDemandId;
    private final String operator;
    private final List<String> receivers;
    private final String name;

    private static final String PRODUCT_DEMAND_TO_RECEIVE_MSG = "### %s  \n  您收到了**%s**抄送的产品需求 **%s**  \n\n  ***  \n  [查看详情](%s)";

    public ProductDemandToCopiedMsgEvent(Object source, Long productDemandId, String operator, List<String> receivers, String name) {
        super(source);
        this.productDemandId = productDemandId;
        this.operator = operator;
        this.receivers = receivers;
        this.name = name;
    }

    @Override
    public void run() {
        String title = MessageTitleEnum.PRODUCT_DEMAND_RECEIVE.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PRODUCT_MANAGEMENT.getText(), productDemandId);
        String markdown = String.format(PRODUCT_DEMAND_TO_RECEIVE_MSG, title, operator, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
