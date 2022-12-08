package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;

import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/23 18:13
 * @Author 望轩
 */
public class BugOnlineConfirmMsgEvent extends MessageEvent {
    private final String BUG_ONLINE_TRANS = "### %s \n【线上bug】**%s** \n\n 此bug不用修复,等待您确认，若不确认，1天后会自动关闭，请及时确认。\n *** \n[点击跳转到线上bug查看页面](%s)  <!--%s-->";

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

    public BugOnlineConfirmMsgEvent(Object source,String bugName, String receiver, Long bugOnlineId) {
        super(source);
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(BUG_ONLINE_TRANS, MessageTitleEnum.BUG_ONLINE_CONFIRM.getText(), bugName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_CONFIRM.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}