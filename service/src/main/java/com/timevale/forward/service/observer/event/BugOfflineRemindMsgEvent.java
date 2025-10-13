package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/10/13 16:17
 * @description:
 */
public class BugOfflineRemindMsgEvent extends MessageEvent {

    private static final String BUG_OFFLINE_REMIND_MSG = "### %s\n 您有一条【线下bug】：%s，状态为**%s**，请及时处理 \n\n  ***\n  [查看详情](%s) ";
    private final Long bugOfflineId;
    private final List<String> receivers;
    private final String name;
    private final String statusName;

    public BugOfflineRemindMsgEvent(Object source, Long bugOfflineId, List<String> receivers, String name, String statusName) {
        super(source);
        this.bugOfflineId = bugOfflineId;
        this.receivers = receivers;
        this.name = name;
        this.statusName = statusName;
    }

    @Override
    public void run() {
        String title = MessageTitleEnum.BUG_OFFLINE_REMIND.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), bugOfflineId);
        String markdown = String.format(BUG_OFFLINE_REMIND_MSG, title, name, statusName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
