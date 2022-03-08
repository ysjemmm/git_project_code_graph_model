package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/02/23 16:37
 */
public class BugOfflineUpdateMsg extends MessageEvent {

    private static final String BUG_OFFLINE_CHANGE_MSG = "### %s\n 您有一条【线下bug】：%s，状态为**%s**，请及时处理 \n\n  ***\n  [查看详情](%s) ";
    private final Long bugOfflineId;
    private final String receiver;
    private final String name;
    private final String statusName;

    public BugOfflineUpdateMsg(Object source, Long bugOfflineId, String receiver, String name, String statusName) {
        super(source);
        this.bugOfflineId = bugOfflineId;
        this.receiver = receiver;
        this.name = name;
        this.statusName = statusName;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BUG_OFFLINE_ADD.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), bugOfflineId);
        String markdown = String.format(BUG_OFFLINE_CHANGE_MSG, title, name, statusName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
