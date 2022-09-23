package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
public class ProjectAcceptanceRemindMsgEvent extends MessageEvent {

    private static final String  ACCEPTANCE_REMIND = "### %s \n **%s**提醒您验收项目**%s**，请及时处理。 \n *** \n[查看详情](%s)  <!--%s-->";

    private static final String ACCEPTANCE_ANCHOR = "&anchor=acceptance";
    /**
     * 操作人
     */
    private final String operator;
    /**
     * 项目id
     */
    private final Long projectId;

    /**
     * 项目名称
     */
    private final String projectName;
    /**
     * 接收人,用户id
     */
    private final List<String> receivers;

    public ProjectAcceptanceRemindMsgEvent(Object source, String operator, Long projectId, String projectName, List<String> receivers) {
        super(source);
        this.operator = operator;
        this.projectId = projectId;
        this.projectName = projectName;
        this.receivers = receivers;
    }

    @Override
    public void run() {
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId)+ACCEPTANCE_ANCHOR;
        String markdown = String.format(ACCEPTANCE_REMIND, MessageTitleEnum.PROJECT_ACCEPTANCE_REMIND.getText(), operator, projectName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.PROJECT_ACCEPTANCE_REMIND.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}