package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/10/13 16:13
 * @description:
 */
public class BugOnlineRemindMsgEvent extends MessageEvent {
    private final String BUG_ONLINE_TRANS = "### %s \n 您有一条【线上bug】：**%s**，优先级为 **%s**，状态为 **%s**，请及时处理。 \n *** \n[查看详情](%s)  <!--%s-->";
    /**
     * bug标题
     */
    private String bugName;
    /**
     * bug状态
     * */
    private String bugStatus;
    /**
     * 接收人,用户id
     */
    private List<String> receivers;
    /**
     * 线上bug id
     */
    private Long bugOnlineId;

    /**
     * 优先级
     */
    private String priority;

    public BugOnlineRemindMsgEvent(Object source, String bugName, String bugStatus, List<String> receivers, Long bugOnlineId, String priority) {
        super(source);
        this.bugName = bugName;
        this.bugStatus = bugStatus;
        this.receivers = receivers;
        this.bugOnlineId = bugOnlineId;
        this.priority = priority;
    }

    @Override
    public void run() {
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(BUG_ONLINE_TRANS, MessageTitleEnum.BUG_ONLINE_REMIND.getText(), bugName, priority, bugStatus, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_REMIND.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}