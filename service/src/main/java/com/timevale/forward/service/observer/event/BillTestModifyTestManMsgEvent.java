package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/2/10 16:13
 * @Author 望轩
 */
public class BillTestModifyTestManMsgEvent extends MessageEvent {
    private final String testBillName;
    private final String testMan;
    private final List<String> receivers;

    public BillTestModifyTestManMsgEvent(Object source, String testBillName, String testMan, List<String> receivers) {
        super(source);
        this.testBillName = testBillName;
        this.testMan = testMan;
        this.receivers = receivers;
    }

    @Override
    public void run() {
        String title = "测试人更新通知";
        String singleUrl = "www.baidu.com";
        String markdown = String.format("### 【提测单】  \n   **%s**测试人改为**%s**。  \n *** \n   [查看详情](%s)   \n   防止内容相同%s",
                testBillName, testMan, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}