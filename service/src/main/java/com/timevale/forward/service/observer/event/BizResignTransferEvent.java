package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2023/05/09 17:12
 */
public class BizResignTransferEvent extends MessageEvent {
    private final Integer count;
    private final String operator;
    private final String receiver;

    private static final String MSG = "### %s  \n  您的下属**%s**已离职 需求接收人已变更由您处理 \n\n  ***  \n  [查看详情](%s)";

    public BizResignTransferEvent(Object source, Integer count, String operator, String receiver) {
        super(source);
        this.count = count;
        this.operator = operator;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = String.format(MessageTitleEnum.BIZDEMAND_RECEIVE_BATCH.getText(), count);
        String singleUrl = domainName + TabEnum.BUSINESS_MANAGEMENT.listTab(1);
        String markdown = String.format(MSG, title, operator, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
