package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/3/2 11:47
 * @Author 望轩
 */
public class BugOfflineOpenAgainMsgEvent extends MessageEvent {
    private final String BUG_OFFLINE_OPEN_AGAIN = "%s重新打开【线下bug】%s，请确认修复，可进入产研项目管理系统查看：%s";
    /**
     * 提交人，花名-真名
     */
    private String proposer;
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

    public BugOfflineOpenAgainMsgEvent(Object source, String proposer, String bugName, String receiver, Long bugOfflineId) {
        super(source);
        this.proposer = proposer;
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOfflineId = bugOfflineId;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), bugOfflineId);
        String markdown = String.format(BUG_OFFLINE_OPEN_AGAIN, proposer, bugName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_OFFLINE_OPEN_AGAIN.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}