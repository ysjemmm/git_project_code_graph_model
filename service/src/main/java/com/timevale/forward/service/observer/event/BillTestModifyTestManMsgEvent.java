package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.model.enums.TestBillMessageTitleEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/2/10 16:13
 * @Author 望轩
 */
public class BillTestModifyTestManMsgEvent extends MessageEvent {
    private final String testBillName;
    private final String testMan;
    private final List<String> receivers;
    private final Long projectId;

    public BillTestModifyTestManMsgEvent(Object source, String testBillName, String testMan, List<String> receivers, Long projectId) {
        super(source);
        this.testBillName = testBillName;
        this.testMan = testMan;
        this.receivers = receivers;
        this.projectId = projectId;
    }

    @Override
    public void run() {
        String title = TestBillMessageTitleEnum.TEST_MAN_UPDATE.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + "&tabActive=3";
        String markdown = String.format("### 【提测单】  \n   **%s**测试人改为**%s**。  \n *** \n   [查看详情](%s)",
                testBillName, testMan, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}