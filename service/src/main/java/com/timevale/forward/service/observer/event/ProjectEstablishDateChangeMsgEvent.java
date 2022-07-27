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
public class ProjectEstablishDateChangeMsgEvent extends MessageEvent {

    private final Long projectId;
    private final String receiver;
    private final String name;
    private final String pjEstablishPublishDate;

    private static final String PUBLISH_DATE_CHANGE_MSG = "### %s  \n  您的项目 **%s** 发布正式计划时间晚于立项预期上线时间 **%s**，请修改发布正式-计划时间。  \n\n  ***  \n  [查看详情](%s)";

    public ProjectEstablishDateChangeMsgEvent(Object source, Long projectId,String receiver, String name, String pjEstablishPublishDate) {
        super(source);
        this.projectId = projectId;
        this.receiver = receiver;
        this.name = name;
        this.pjEstablishPublishDate = pjEstablishPublishDate;
    }

    @Override
    public void run() {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.PROJECT_ESTABLISH_DATE_CHANGE.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId);
        String markdown = String.format(PUBLISH_DATE_CHANGE_MSG, title, name, pjEstablishPublishDate, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
