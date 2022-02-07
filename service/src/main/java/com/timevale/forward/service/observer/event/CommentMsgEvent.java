package com.timevale.forward.service.observer.event;

import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import com.timevale.forward.service.utils.StringUtil;

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
        } else if (CommentTypeEnum.BIZ_DEMAND.getText().equals(type)){
            singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_MANAGEMENT.getText(), mainId);
        } else {
            singleUrl = domainName + String.format(PARAM, TabEnum.TASK_MANAGEMENT.getText(), mainId);
        }
        String markdown = String.format(COMMENT_MSG, title, operator, type, name, content, singleUrl);

        MarkdownMsg markdownMsg = MarkdownMsg.builder()
                .title(title)
                .content(markdown)
                .receivers(receivers)
                .build();

        erpMessageClient.sendMarkdownMsg(markdownMsg);
    }
}
