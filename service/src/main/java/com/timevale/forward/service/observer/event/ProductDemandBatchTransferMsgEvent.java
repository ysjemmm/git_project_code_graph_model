package com.timevale.forward.service.observer.event;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;

/**
 * @author by xingyun
 * @date 2022/11/22 11:20
 */
public class ProductDemandBatchTransferMsgEvent extends MessageEvent {

    private final String initiator;
    private final String receiver;
    private final Integer count;

    private static final String MSG = "### %s  \n  **%s**批量转交您**%s**个需求，请及时处理。 \n\n  ***  \n  [查看详情](%s) <!--%s-->";

    public ProductDemandBatchTransferMsgEvent(Object source, String initiator, String receiverAccount, Integer count) {
        super(source);
        this.initiator = initiator;
        this.receiver = receiverAccount;
        this.count = count;
    }

    @Override
    public void run() {
        String title = MessageTitleEnum.PD_BATCH_TRANSFER.getText();
        String singleUrl = domainName + TabEnum.PRODUCT_MANAGEMENT.listTab(0);
        String markdown = String.format(MSG, title, initiator, count, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(CollUtil.newArrayList(receiver))
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
