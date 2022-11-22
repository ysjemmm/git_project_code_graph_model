package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by xingyun
 * @date 2022/11/22 11:20
 */
public class BizDemandApprovedMsgEvent extends MessageEvent {

    private final Long bizDemandId;
    private final String receiver;
    private final String name;

    private static final String BIZ_DEMAND_RECEIVED_MSG = "### %s  \n  【业务需求】 **%s**，内部资源已申请通过，请知悉。  \n\n  ***  \n  [查看详情](%s)";

    public BizDemandApprovedMsgEvent(Object source, Long bizDemandId, String receiver, String name) {
        super(source);
        this.bizDemandId = bizDemandId;
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_APPROVED.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), bizDemandId);
        String markdown = String.format(BIZ_DEMAND_RECEIVED_MSG, title, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
