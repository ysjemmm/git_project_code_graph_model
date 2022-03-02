package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/3/2 11:23
 * @Author 望轩
 */
public class BugOfflineDelayRepairMsgEvent extends MessageEvent {
    private final String BUG_OFFLINE_DELAY_REPAIR = "%s延期修复【线下bug】%s，请查看延期修复原因，可进入产研项目管理系统查看：%s";
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

    public BugOfflineDelayRepairMsgEvent(Object source, String operator, String bugName, String receiver, Long bugOfflineId) {
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
        String markdown = String.format(BUG_OFFLINE_DELAY_REPAIR, operator, bugName, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_OFFLINE_DELAY_REPAIR.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}