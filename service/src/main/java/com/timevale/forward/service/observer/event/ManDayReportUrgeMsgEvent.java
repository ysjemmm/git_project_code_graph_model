package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/08/23 16:40
 */
public class ManDayReportUrgeMsgEvent extends MessageEvent {

    private final String operator;
    private final String receiver;

    private static final String MSG = "### %s  \n  **%s**提醒您尽快处理项目人天提报申请  \n\n  ***  \n  [查看详情](%s)";

    public ManDayReportUrgeMsgEvent(Object source, String operator, String receiver) {
        super(source);
        this.operator = operator;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.MAN_DAY_URGE.getText();
        String singleUrl = domainName ;
        String markdown = String.format(MSG, title, operator, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
