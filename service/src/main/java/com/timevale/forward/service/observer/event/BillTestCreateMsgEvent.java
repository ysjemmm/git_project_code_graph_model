package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.model.enums.TestBillMessageTitleEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/1/26 14:47
 * @Author 望轩
 */
public class BillTestCreateMsgEvent extends MessageEvent {

    private final String operator;
    private final List<String> receivers;
    private final String billTestName;
    private final Long projectId;

    public BillTestCreateMsgEvent(Object source, String operator, List<String> receivers, String billTestName, Long projectId) {
        super(source);
        this.operator = operator;
        this.receivers = receivers;
        this.billTestName = billTestName;
        this.projectId = projectId;
    }

    @Override
    public void run() {
        String title = TestBillMessageTitleEnum.SUBMIT_SMOKING_TEST.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + "&tabActive=3";
        String markdown = String.format("%s \n  **%s**发起了**%s**,请前往提交测试用例。   \n *** \n   [查看详情](%s)",
                title,operator, billTestName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}