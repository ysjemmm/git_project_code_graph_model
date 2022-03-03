package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/2/25 19:17
 * @Author 望轩
 */
public class BugOfflineSelfTestPassMsgEvent extends MessageEvent {
    private final String BUG_OFFLINE_SELF_PASS = "### %s \n **%s**自测通过【线下bug】**%s**,请验收。\n *** \n[查看详情](%s)";
    /**
     * 操作人，花名-真名
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
     * 线下bug id
     */
    private Long bugOfflineId;

    public BugOfflineSelfTestPassMsgEvent(Object source, String operator, String bugName, String receiver, Long bugOfflineId) {
        super(source);
        this.operator = operator;
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOfflineId = bugOfflineId;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), bugOfflineId);
        String markdown = String.format(BUG_OFFLINE_SELF_PASS, MessageTitleEnum.BUG_OFFLINE_CHECK.getText(), operator, bugName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_OFFLINE_CHECK.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}