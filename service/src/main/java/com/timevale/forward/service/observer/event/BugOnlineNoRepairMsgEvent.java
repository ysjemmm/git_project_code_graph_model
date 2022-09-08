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
public class BugOnlineNoRepairMsgEvent extends MessageEvent {
    private final String BUG_ONLINE_TRANS = "### %s \n **%s**申请不用修复【线上bug】**%s**，等待您确认，若不确认，7天后会自动关闭。 \n *** \n[查看详情](%s) \n <!--%s-->";
    /**
     * 操作人
     */
    private String operator;
    /**
     * bug标题
     */
    private String bugName;
    /**
     * 接收人,用户id
     */
    private String receiver;
    /**
     * 线上bug id
     */
    private Long bugOnlineId;

    public BugOnlineNoRepairMsgEvent(Object source, String operator, String bugName, String receiver, Long bugOnlineId) {
        super(source);
        this.operator = operator;
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(BUG_ONLINE_TRANS, MessageTitleEnum.BUG_ONLINE_NO_REPAIR.getText(), operator, bugName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_NO_REPAIR.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}