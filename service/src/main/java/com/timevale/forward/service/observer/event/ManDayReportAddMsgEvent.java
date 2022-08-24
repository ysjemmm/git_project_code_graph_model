package com.timevale.forward.service.observer.event;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/08/23 16:40
 */
public class ManDayReportAddMsgEvent extends MessageEvent {

    private final String operator;
    private final String receiver;
    private final String time;
    private final String project;
    private final String manDay;

    private static final String LINK = "%s/auditList";
    private static final String MSG = "### %s  \n  %s提报%s项目%s实际工时：%s人天，请及时审批  \n\n  ***  \n  [查看详情](%s)";

    public ManDayReportAddMsgEvent(Object source, String operator, String receiver, String time, String project, String manDay) {
        super(source);
        this.operator = operator;
        this.receiver = receiver;
        this.time = time;
        this.project = project;
        this.manDay = manDay;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.MAN_DAY_AUDIT.getText();
        String singleUrl = domainName + String.format(LINK, TabEnum.MAN_DAY_MANAGEMENT.getText());
        String markdown = String.format(MSG, title, operator, time, project,manDay, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();
        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
