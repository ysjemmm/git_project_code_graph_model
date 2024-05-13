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
public class BugOnlineResignTransferEvent extends MessageEvent {
    private final String operator;
    private final String receiver;

    private static final String MSG = "### %s  \n  您的下属**%s**已离职，线上bug已变更由您处理 \n\n  ***  \n  [查看详情](%s)";

    public BugOnlineResignTransferEvent(Object source, String operator, String receiver) {
        super(source);
        this.operator = operator;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = String.format(MessageTitleEnum.BUG_ONLINE_TRANSFER.getText());
        String singleUrl = domainName + TabEnum.BUG_ONLINE_MANAGEMENT.listTab(0);
        String markdown = String.format(MSG, title, operator, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
