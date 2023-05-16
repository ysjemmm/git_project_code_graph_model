package com.timevale.forward.service.observer.event;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

/**
 * @author by xingyun
 * @date 2022/11/22 11:20
 */
public class SrEvalEndMsgEvent extends MessageEvent {

    private final Long projectId;
    private final String receiver;
    private final String name;

    private static final String MSG = "### %s  \n  产研项目 **%s** SFR已提交项目评价，请及时发起结项。 \n\n  ***  \n  [查看详情](%s)";

    public SrEvalEndMsgEvent(Object source, Long projectId, String receiver, String name) {
        super(source);
        this.projectId = projectId;
        this.receiver = receiver;
        this.name = name;
    }

    @Override
    public void run() {
        String title = MessageTitleEnum.PROJECT_PUBLISH_EVAL.getText();
        String singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), projectId) + "&tabActive=11";
        String markdown = String.format(MSG, title, name, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(CollUtil.newArrayList(receiver))
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
