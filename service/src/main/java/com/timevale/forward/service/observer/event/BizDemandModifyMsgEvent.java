package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/08/08 14:53
 */
public class BizDemandModifyMsgEvent extends MessageEvent {

    private final Long bizDemandId;
    private final String operator;
    private final String receiver;
    private final String name;

    private static final String BIZ_DEMAND_INVALID_MSG = "### %s  \n  您收到了**%s**提交的业务需求**%s**发生了修改  \n\n ***  \n  [查看详情](%s)";

    public BizDemandModifyMsgEvent(Object source, Long bizDemandId, String operator, String receiver, String name) {
        super(source);
        this.bizDemandId = bizDemandId;
        this.operator = operator;
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_MODIFY.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), bizDemandId);
        String markdown = String.format(BIZ_DEMAND_INVALID_MSG, title, operator, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
