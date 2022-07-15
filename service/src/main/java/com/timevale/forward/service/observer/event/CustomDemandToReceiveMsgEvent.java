package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/21 15:27
 */
public class CustomDemandToReceiveMsgEvent extends MessageEvent {

    private final Long customDemandId;
    private final String operator;
    private final String receiver;
    private final String name;

    //您收到一条新的客户需求
    // 您收到了{客户名称-需求提交人}提交的客户需求 {需求主题}
    private static final String CUSTOMDEMAND_RECEIVE = "### %s  \n  您收到了**%s**提交的客户需求 **%s**  \n\n  ***  \n  [查看详情](%s)";

    public CustomDemandToReceiveMsgEvent(Object source, Long customDemandId, String operator, String receiver, String name) {
        super(source);
        this.customDemandId = customDemandId;
        this.operator = operator;
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.CUSTOMDEMAND_RECEIVE.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.CUSTOM_MANAGEMENT.getText(), customDemandId);
        String markdown = String.format(CUSTOMDEMAND_RECEIVE, title, operator, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
