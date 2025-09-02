package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/21 15:27
 */
public class BizDemandToCopiedMsgEvent extends MessageEvent {

    private final Long bizDemandId;
    private final String operator;
    private final List<String> receivers;
    private final String name;

    private static final String BIZ_DEMAND_TO_RECEIVE_MSG = "### %s  \n  您收到了**%s**抄送的业务需求 **%s**  \n\n  ***  \n  [查看详情](%s)";

    public BizDemandToCopiedMsgEvent(Object source, Long bizDemandId, String operator, List<String> receivers, String name) {
        super(source);
        this.bizDemandId = bizDemandId;
        this.operator = operator;
        this.receivers = receivers;
        this.name = name;
    }

    @Override
    public void run() {
        String title = MessageTitleEnum.BIZDEMAND_RECEIVE.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), bizDemandId);
        String markdown = String.format(BIZ_DEMAND_TO_RECEIVE_MSG, title, operator, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
