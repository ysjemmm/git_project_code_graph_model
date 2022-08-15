package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/23 16:05
 * @Author 望轩
 */
public class BugOnlineResubmitNoRepairMsgEvent extends MessageEvent {
    private static final String BUG_ONLINE_NO_REPAIR = "### %s \n 相关的线上bug**%s**申请不用修复，请知悉 。 \n *** \n[查看详情](%s) \n <!--%s-->";
    /**
     * bug标题
     */
    private final String bugName;
    /**
     * 接收人,用户id
     */
    private final String receiver;
    /**
     * 线上bug id
     */
    private final Long bugOnlineId;

    public BugOnlineResubmitNoRepairMsgEvent(Object source, String bugName, String receiver, Long bugOnlineId) {
        super(source);
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(BUG_ONLINE_NO_REPAIR, MessageTitleEnum.BUG_ONLINE_RESUBMIT_NO_REPAIR.getText(), bugName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_RESUBMIT_NO_REPAIR.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}