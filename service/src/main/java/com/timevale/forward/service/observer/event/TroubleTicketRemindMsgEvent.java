package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;

import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/23 18:13
 * @Author 望轩
 */
public class TroubleTicketRemindMsgEvent extends MessageEvent {
    private final String TROUBLE_TICKET_TRANS = "### %s \n【发送时间】**%s** \n\n 【故障概述】**%s** \n\n 有**%s**个改进措施未处理完成，请及时处理。 \n *** \n[查看详情](%s)  <!--%s-->";

    /**
     * 故障概述
     */
    private String troubleTicketName;

    /**
     * 发送时间
     */
    private String sendDate;

    /**
     * 接收人,用户id
     */
    private String receiver;
    /**
     * 故障 id
     */
    private Long troubleTicketId;

    /**
     * 改进措施个数
     */
    private Integer improvementMeasureCount;

    public TroubleTicketRemindMsgEvent(Object source, String troubleTicketName, String receiver, Long troubleTicketId,String sendDate,Integer improvementMeasureCount) {
        super(source);
        this.troubleTicketName = troubleTicketName;
        this.receiver = receiver;
        this.troubleTicketId = troubleTicketId;
        this.improvementMeasureCount = improvementMeasureCount;
        this.sendDate = sendDate;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String singleUrl = domainName + String.format(PARAM, TabEnum.TROUBLE_MANAGEMENT.getText(), troubleTicketId);
        String markdown = String.format(TROUBLE_TICKET_TRANS, MessageTitleEnum.TROUBLE_TICKET_REMIND.getText(), sendDate,troubleTicketName,improvementMeasureCount, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.TROUBLE_TICKET_REMIND.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}