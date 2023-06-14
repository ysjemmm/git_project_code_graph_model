package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * 线上bug用户临时解决方案调整通知
 *
 * @author by YangXu
 * @date 2023/06/14 18:03
 */
public class BugOnlineTemporarySolutionMsg extends MessageEvent {
    private final String bugName;
    private final String receiver;
    private final Long bugOnlineId;

    public BugOnlineTemporarySolutionMsg(Object source, String bugName, String receiver, Long bugOnlineId) {
        super(source);
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
    }

    @Override
    public void run() {
        String bugOnlineTrans = "### %s \n 线上bug: **%s** 调整了用户临时解决方案，请及时查看。 \n *** \n[查看详情](%s)  <!--%s-->";

        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(bugOnlineTrans, MessageTitleEnum.BUG_ONLINE_TEMPORARY_SOLUTION.getText(), bugName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_TEMPORARY_SOLUTION.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}