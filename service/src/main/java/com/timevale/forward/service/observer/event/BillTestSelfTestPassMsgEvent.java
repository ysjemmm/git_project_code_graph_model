package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/2/10 16:46
 * @Author 望轩
 */
public class BillTestSelfTestPassMsgEvent extends MessageEvent {
    private final String operator;
    private final String billTestName;
    private final List<String> receivers;

    public BillTestSelfTestPassMsgEvent(Object source, String operator, String billTestName, List<String> receivers) {
        super(source);
        this.operator = operator;
        this.billTestName = billTestName;
        this.receivers = receivers;
    }

    @Override
    public void run() {
        String title = "提测预演通知";
        String singleUrl = "www.baidu.com";
        String markdown = String.format("### 【提测单】 \n  **%s**自测通过**%s**，请验收提测预演。  \n *** \n   [查看详情](%s)   \n   防止内容相同%s",
                operator, billTestName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}