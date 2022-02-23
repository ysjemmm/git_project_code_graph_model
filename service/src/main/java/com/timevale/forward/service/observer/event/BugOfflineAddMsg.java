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
public class BugOfflineAddMsg extends MessageEvent{

    private final Long bugOfflineId;
    private final String operator;
    private final String receiver;
    private final String name;

    private static final String BUG_OFFLINE_ADD_MSG = "### %s\n **%s** 创建了【线下bug】%s，请确认修复 \n\n  ***\n  [查看详情](%s)";

    public BugOfflineAddMsg(Object source, Long bugOfflineId, String operator, String receiver, String name) {
        super(source);
        this.bugOfflineId = bugOfflineId;
        this.operator = operator;
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BUG_OFFLINE_ADD.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), bugOfflineId);
        String markdown = String.format(BUG_OFFLINE_ADD_MSG, title, operator, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
