package com.timevale.forward.service.observer.event;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;


/**
 * @author by YangXu
 * @date 2023/05/11 16:47
 */
public class BugOnlineAcceptanceFailMsgEvent extends MessageEvent {
    private final String bugName;
    private final String receiver;
    private final Long bugOnlineId;

    public BugOnlineAcceptanceFailMsgEvent(Object source, String bugName, String receiver, Long bugOnlineId) {
        super(source);
        this.bugName = bugName;
        this.receiver = receiver;
        this.bugOnlineId = bugOnlineId;
    }

    @Override
    public void run() {
        String msg = "### %s \n 线上bug **%s** 验收不通过，请及时处理。 \n *** \n[查看详情](%s)  <!--%s-->";
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), bugOnlineId);
        String markdown = String.format(msg, MessageTitleEnum.BUG_ONLINE_ACCEPTANCE_FAIL.getText(), bugName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.BUG_ONLINE_ACCEPTANCE_FAIL.getText())
                .content(markdown)
                .receivers(CollUtil.newArrayList(receiver))
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}