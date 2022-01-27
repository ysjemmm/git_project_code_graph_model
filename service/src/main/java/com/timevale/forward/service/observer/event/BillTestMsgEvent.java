package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/1/26 14:47
 * @Author 望轩
 */
public class BillTestMsgEvent extends MessageEvent {

    private final String operator;
    private final String receiver;

    public BillTestMsgEvent(Object source, String operator, String receiver) {
        super(source);
        this.operator = operator;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = "XX提测单XX";
        String singleUrl = "www.baidu.com";
        String markdown = String.format("%s测一下看看行不行[查看详情](%s)", operator, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}