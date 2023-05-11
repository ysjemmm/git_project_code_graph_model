package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/05/06 17:28
 */
public class BugOnlineToBizAgreeEvent extends MessageEvent {
    private final String bugName;
    private final String operator;
    private final String receiver;
    private final Long bugOnlineId;

    public BugOnlineToBizAgreeEvent(Object source, String operator, String bugName, String receiver, Long bugOnlineId) {
        super(source);
        this.bugName = bugName;
        this.operator = operator;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
    }

    @Override
    public void run() {
        String BUG_ONLINE_TRANS = "### %s \n **%s** 同意对线上bug **%s** 转业务需求 \n *** \n[查看详情](%s)  <!--%s-->";

        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(BUG_ONLINE_TRANS, MessageTitleEnum.BUG_ONLINE_TO_BIZ_AGREE.getText(), operator, bugName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_TO_BIZ_AGREE.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}