package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/23 17:13
 * @Author 望轩
 */
public class BugOnlineTransferMsgEvent extends MessageEvent {
    private final String BUG_ONLINE_TRANS = "### %s \n **%s**转交给您一条【线上bug】：**%s**，优先级为 **%s**，状态为 **%s**，请及时处理。 \n *** \n[查看详情](%s)  <!--%s-->";
    /**
     * 操作人
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
     * 线上bug id
     */
    private Long bugOnlineId;
    /**
     * 优先级
     */
    private String priority;

    public BugOnlineTransferMsgEvent(Object source, String operator, String bugName, String bugStatus, String receiver, Long bugOnlineId, String priority) {
        super(source);
        this.operator = operator;
        this.bugName = bugName;
        this.bugStatus = bugStatus;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
        this.priority = priority;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(BUG_ONLINE_TRANS, MessageTitleEnum.BUG_ONLINE_TRANSFER.getText(), operator, bugName, priority, bugStatus, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_TRANSFER.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}