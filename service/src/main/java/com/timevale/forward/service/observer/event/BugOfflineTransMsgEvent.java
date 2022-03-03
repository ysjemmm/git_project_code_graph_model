package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @Date 2022/3/2 11:35
 * @Author 望轩
 */
public class BugOfflineTransMsgEvent extends MessageEvent {
    private final String BUG_OFFLINE_TRANS = "### %s \n **%s**转交给您一条【线下bug】：**%s**，状态为**%s**，请及时处理。 \n *** \n[查看详情](%s)";
    /**
     * 操作人，花名-真名
     */
    private String operator;
    /**
     * bug标题
     */
    private String bugName;
    /**
     * bug状态
     */
    private String bugStatus;
    /**
     * 接收人,用户id
     */
    private String receiver;
    /**
     * 线下bug id
     */
    private Long bugOfflineId;

    public BugOfflineTransMsgEvent(Object source, String operator, String bugName, String bugStatus, String receiver, Long bugOfflineId) {
        super(source);
        this.operator = operator;
        this.bugName = bugName;
        this.bugStatus = bugStatus;
        this.receiver = receiver;
        this.bugOfflineId = bugOfflineId;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), bugOfflineId);
        String markdown = String.format(BUG_OFFLINE_TRANS, MessageTitleEnum.BUG_OFFLINE_TRANS.getText(), operator, bugName, bugStatus, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_OFFLINE_TRANS.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}