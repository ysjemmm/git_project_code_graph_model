package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/21 15:20
 */
public class ProjectNodeDelayUnInputMsgEvent extends MessageEvent {

    private final Long projectId;
    private final String receiver;
    private final String name;
    private final String planDate;

    private static final String PROJECT_NODE_DELAY_UNINPUT_MSG = "### %s  \n  **%s**节点计划时间为**%s**已逾期，请及时录入。  \n\n  ***  \n  [查看详情](%s)";

    public ProjectNodeDelayUnInputMsgEvent(Object source, Long projectId, String receiver, String name, String planDate) {
        super(source);
        this.projectId = projectId;
        this.receiver = receiver;
        this.name = name;
        this.planDate = planDate;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.PROJECT_NODE_DELAY_UNINPUT.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId);
        String markdown = String.format(PROJECT_NODE_DELAY_UNINPUT_MSG, title, name, planDate, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
