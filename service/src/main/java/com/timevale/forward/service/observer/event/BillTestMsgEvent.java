package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/1/26 14:47
 * @Author 望轩
 */
public class BillTestMsgEvent extends MessageEvent {

    private final String operator;
    private final List<String> receivers;

    public BillTestMsgEvent(Object source, String operator, List<String> receivers) {
        super(source);
        this.operator = operator;
        this.receivers = receivers;
    }

    @Override
    public void run() {
        String title = "XX提测单XX";
        String singleUrl = "www.baidu.com";
        String markdown = String.format("%s测一下看%s看行不行[查看详情](%s)", operator, new Date(), singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}