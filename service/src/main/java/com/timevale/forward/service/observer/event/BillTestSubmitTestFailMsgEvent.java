package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.model.enums.TestBillMessageTitleEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/2/10 17:28
 * @Author 望轩
 */
public class BillTestSubmitTestFailMsgEvent extends MessageEvent {
    private final String billTestName;
    private final List<String> receivers;
    private final Long projectId;

    public BillTestSubmitTestFailMsgEvent(Object source, String billTestName, List<String> receivers, Long projectId) {
        super(source);
        this.receivers = receivers;
        this.billTestName = billTestName;
        this.projectId = projectId;
    }

    @Override
    public void run() {
        String title = TestBillMessageTitleEnum.SUBMIT_TEST_FAIL.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + "&tabActive=3";
        String markdown = String.format("### 【提测单】 \n  **%s**提测失败,请重新自测。  \n *** \n   [查看详情](%s)",
                billTestName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}