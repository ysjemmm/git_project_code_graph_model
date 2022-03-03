package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/3/2 11:14
 * @Author 望轩
 */
public class BugOfflineCheckFailMsgEvent extends MessageEvent {
    private final String BUG_OFFLINE_CHECK_FAIL = "### %s\n 您的【线下bug】验收失败：**%s**，请及时处理。\n ***\n [查看详情](%s)";

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

    public BugOfflineCheckFailMsgEvent(Object source, String bugName, String receiver, Long bugOfflineId) {
        super(source);
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOfflineId = bugOfflineId;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), bugOfflineId);
        String markdown = String.format(BUG_OFFLINE_CHECK_FAIL, MessageTitleEnum.BUG_OFFLINE_CHECK_FAIL.getText(), bugName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_OFFLINE_CHECK_FAIL.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}