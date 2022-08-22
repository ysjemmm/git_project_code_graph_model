package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/17 15:47
 */
public class TrackEventApprovalMsgEvent extends MessageEvent {

    private final String receiver;
    private final Integer count;
    private final String operator;


    private static final String TRACK_EVENT_APPROVAL_MSG = "### %s  \n  **%s** 提交了**%s**条埋点事件，请前往运营支撑系统-工作台，及时处理。  \n\n ***  \n  [查看详情](%s)";

    public TrackEventApprovalMsgEvent(Object source, String operator, String receiver, Integer count) {
        super(source);
        this.operator = operator;
        this.receiver = receiver;
        this.count = count;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.TRACK_EVENT_APPROVAL.getText();

        String singleUrl = config.getWorkflowBaseUrl();
        String markdown = String.format(TRACK_EVENT_APPROVAL_MSG, title, operator, count, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();
        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
