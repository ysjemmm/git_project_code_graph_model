package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.model.enums.TestBillMessageTitleEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/2/10 16:46
 * @Author 望轩
 */
public class BillTestSelfTestPassMsgEvent extends MessageEvent {
    private final String operator;
    private final String billTestName;
    private final List<String> receivers;
    private final Long projectId;

    public BillTestSelfTestPassMsgEvent(Object source, String operator, String billTestName, List<String> receivers, Long projectId) {
        super(source);
        this.operator = operator;
        this.billTestName = billTestName;
        this.receivers = receivers;
        this.projectId = projectId;
    }

    @Override
    public void run() {
        String title = TestBillMessageTitleEnum.SUBMIT_TEST_SHOW.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + "&tabActive=3";
        String markdown = String.format("### 【提测单】 \n  **%s**自测通过**%s**，请验收提测预演。  \n *** \n   [查看详情](%s)",
                operator, billTestName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}