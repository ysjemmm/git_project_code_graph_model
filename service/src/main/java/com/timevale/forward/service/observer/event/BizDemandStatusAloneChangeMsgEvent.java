package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import com.timevale.forward.service.utils.date.DateUtil;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/21 15:26
 */
public class BizDemandStatusAloneChangeMsgEvent extends MessageEvent {

    private final Long bizDemandId;
    private final String receiver;
    private final String name;
    private final String status;

    private static final String BIZ_DEMAND_STATUS_CHANGE_MSG = "### %s  \n  您提交的业务需求 **%s** 状态已变为 **%s**  \n\n  ***  \n  [查看详情](%s)";

    public BizDemandStatusAloneChangeMsgEvent(Object source, Long bizDemandId, String receiver, String name, String status) {
        super(source);
        this.bizDemandId = bizDemandId;
        this.receiver = receiver;
        this.name = name;
        this.status = status;
    }

    @Override
    public void run() {

        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_STATUS_CHANGE.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), bizDemandId);
        String markdown = String.format(BIZ_DEMAND_STATUS_CHANGE_MSG, title, name, status, singleUrl);
        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
