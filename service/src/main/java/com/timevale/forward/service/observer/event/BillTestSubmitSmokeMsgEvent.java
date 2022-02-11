package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.model.enums.TestBillMessageTitleEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/2/10 15:02
 * @Author 望轩
 */
public class BillTestSubmitSmokeMsgEvent extends MessageEvent {
    private final String operator;
    private final List<String> receivers;
    private final String billTestName;
    private final Long projectId;

    public BillTestSubmitSmokeMsgEvent(Object source, String operator, List<String> receivers, String billTestName, Long projectId) {
        super(source);
        this.operator = operator;
        this.receivers = receivers;
        this.billTestName = billTestName;
        this.projectId = projectId;
    }

    @Override
    public void run() {
        String title = TestBillMessageTitleEnum.SELF_TEST.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + "&tabActive=3";
        String markdown = String.format("### 【提测单】 \n  **%s**提起了**%s**冒烟用例，请自测。  \n *** \n   [查看详情](%s)",
                operator, billTestName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}