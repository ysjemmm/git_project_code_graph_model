package com.timevale.forward.service.observer.event;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;

/**
 * @Date 2022/3/21 17:48
 * @Author 望轩
 */
public class BugOnlineAcceptanceMsgEvent extends MessageEvent {
    private final String bugName;
    private final String receiver;
    private final Long bugOnlineId;

    public BugOnlineAcceptanceMsgEvent(Object source, String bugName, String receiver, Long bugOnlineId) {
        super(source);
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
    }

    @Override
    public void run() {
        String msg = "### %s \n 您的【线上bug】已完成：**%s**。 \n *** \n[查看详情](%s)  <!--%s-->";
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(msg, MessageTitleEnum.BUG_ONLINE_ONLINE.getText(), bugName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_ONLINE.getText())
                .content(markdown)
                .receivers(CollUtil.newArrayList(receiver))
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}