package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/21 15:28
 */
public class CommentMsgEvent extends MessageEvent {

    private final Long mainId;
    private final String operator;
    private final List<String> receivers;
    private final String type;
    private final String name;
    private final String content;

    private static final String COMMENT_ANCHOR = "&anchor=comment";
    private static final String COMMENT_MSG = "### %s  \n  **%s**评论了%s **%s**  \n  > %s  \n\n  ***  \n  [查看详情](%s)";

    public CommentMsgEvent(Object source, Long mainId, String operator, List<String> receivers, String type, String name, String content) {
        super(source);
        this.mainId = mainId;
        this.operator = operator;
        this.receivers = receivers;
        this.type = type;
        this.name = name;
        this.content = content;
    }

    @Override
    public void run() {
        String title = type + MessageTitleEnum.COMMENT.getText();
        String singleUrl;
        if (CommentTypeEnum.PROJECT.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.PRODUCT_DEMAND.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.PRODUCT_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.BIZ_DEMAND.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.TASK.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.TASK_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.BUG_OFFLINE.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.BUG_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.BUG_ONLINE.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.BUG_ONLINE_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.TROUBLE_TICKET.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.TROUBLE_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.CUSTOM_DEMAND.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.CUSTOM_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.INNER_PROJECT.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.INNER_PROJECT_MANAGEMENT.getText(), mainId);
        } else if (CommentTypeEnum.INNER_TASK.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.INTERNAL_TASK_MANAGEMENT.getText(), mainId);
        } else {
            return;
        }

        // 评论添加定位
        singleUrl += COMMENT_ANCHOR;

        String markdown = String.format(COMMENT_MSG, title, operator, type, name, content, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
