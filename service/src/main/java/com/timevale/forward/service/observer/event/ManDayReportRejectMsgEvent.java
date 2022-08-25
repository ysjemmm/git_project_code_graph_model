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
public class ManDayReportRejectMsgEvent extends MessageEvent {

    private final String operator;
    private final String receiver;
    private final String time;
    private final String project;
    private final String manDay;
    private final String rejectReason;

    private static final String LINK = "%s/auditList?tabActive=1";
    private static final String MSG = "### %s  \n  **%s**驳回了您提报的 **%s**项目**%s**，实际工时：**%s**人天，拒绝原因：**%s**，请重新提报  \n\n  ***  \n  [查看详情](%s)";

    public ManDayReportRejectMsgEvent(Object source, String operator, String receiver, String time, String project, String manDay, String rejectReason) {
        super(source);
        this.operator = operator;
        this.receiver = receiver;
        this.time = time;
        this.project = project;
        this.manDay = manDay;
        this.rejectReason = rejectReason;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.MAN_DAY_REJECT.getText();
        String singleUrl = domainName + String.format(LINK, TabEnum.MAN_DAY_MANAGEMENT.getText());
        String markdown = String.format(MSG, title, operator, time, project, manDay, rejectReason, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
