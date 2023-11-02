package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.Date;
import java.util.List;


/**
 * 项目中止消息事件
 *
 * @author yangxu
 * @date 2023/10/31
 */
public class ProjectInvalidMsgEvent extends MessageEvent {

    private static final String ACCEPTANCE_START = "### %s \n 您参与的产研项目：**%s**，状态为 **已中止**，请知悉。 \n *** \n[查看详情](%s)  <!--%s-->";

    private static final String ACCEPTANCE_ANCHOR = "&anchor=acceptance";
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

    public ProjectInvalidMsgEvent(Object source, Long projectId, String projectName, List<String> receivers) {
        super(source);
        this.projectId = projectId;
        this.projectName = projectName;
        this.receivers = receivers;
    }

    @Override
    public void run() {
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + ACCEPTANCE_ANCHOR;
        String markdown = String.format(ACCEPTANCE_START, MessageTitleEnum.PROJECT_INVALID.getText(), projectName, singleUrl, new Date());

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(MessageTitleEnum.PROJECT_INVALID.getText())
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}