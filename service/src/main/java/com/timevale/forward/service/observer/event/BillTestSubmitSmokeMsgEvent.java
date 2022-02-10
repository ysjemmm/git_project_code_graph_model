package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/2/10 15:02
 * @Author 望轩
 */
public class BillTestSubmitSmokeMsgEvent extends MessageEvent {
    private final String operator;
    private final List<String> receivers;
    private final String billTestName;

    public BillTestSubmitSmokeMsgEvent(Object source, String operator, List<String> receivers, String billTestName) {
        super(source);
        this.operator = operator;
        this.receivers = receivers;
        this.billTestName = billTestName;
    }

    @Override
    public void run() {
        String title = "自测通知";
        String singleUrl = "www.baidu.com";
        String markdown = String.format("### 【提测单】 \n  **%s**提起了**%s**冒烟用例，请自测。  \n *** \n   [查看详情](%s)   \n   防止内容相同%s",
                operator, billTestName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}