package com.timevale.forward.service.observer.event;

import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/2/10 17:18
 * @Author 望轩
 */
public class BillTestSubmitTestSuccessMsgEvent extends MessageEvent {
    private final String billTestName;
    private final List<String> receivers;

    public BillTestSubmitTestSuccessMsgEvent(Object source, String billTestName, List<String> receivers) {
        super(source);
        this.receivers = receivers;
        this.billTestName = billTestName;
    }

    @Override
    public void run() {
        String title = "提测成功通知";
        String singleUrl = "www.baidu.com";
        String markdown = String.format("### 【提测单】 \n  **%s**提测成功。  \n *** \n   [查看详情](%s)   \n   防止内容相同%s",
                billTestName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}