package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/21 15:20
 */
public class BizDemandReceivedMsgEvent extends MessageEvent {

    private final Long bizDemandId;
    private final String operator;
    private final String receiver;
    private final String name;
    private final String planReleaseDate;

    private static final String BIZ_DEMAND_RECEIVED_MSG = "### %s  \n  **%s**接收了您提交的业务需求 **%s**，预期上线时间为 **%s**  \n\n  ***  \n  [查看详情](%s)";

    public BizDemandReceivedMsgEvent(Object source, Long bizDemandId, String operator, String receiver, String name, String planReleaseDate) {
        super(source);
        this.bizDemandId = bizDemandId;
        this.operator = operator;
        this.receiver = receiver;
        this.name = name;
        this.planReleaseDate = planReleaseDate;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_FEEDBACK.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), bizDemandId);
        String markdown = String.format(BIZ_DEMAND_RECEIVED_MSG, title, operator, name, planReleaseDate, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
