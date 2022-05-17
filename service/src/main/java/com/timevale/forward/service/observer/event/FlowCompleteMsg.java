package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/02/23 16:37
 */
public class FlowCompleteMsg extends MessageEvent {

    private final List<String> receivers;
    private final String projectName;
    private final Long projectId;

    public FlowCompleteMsg(Object source, List<String> receivers, String projectName, Long projectId) {
        super(source);
        this.receivers = receivers;
        this.projectName = projectName;
        this.projectId = projectId;
    }

    @Override
    public void run() {
//        String title = TestBillMessageTitleEnum.SUBMIT_SMOKING_TEST.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + "&tabActive=1";
        String markdown = String.format("###  \n  您发起的%s详设评审已全部评审通过，可进入产研项目管理系统查看   \n *** \n   [查看详情](%s)",
                projectName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
