package com.timevale.forward.service.component.impl;

import com.google.common.collect.Lists;
import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.model.enums.TabEnum;
import com.timevale.forward.service.component.MessageComponent;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.utils.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/29 14:27
 */
@Component
@Slf4j
public class MessageComponentImpl implements MessageComponent {

    @Resource
    ErpMessageClient erpMessageClient;

    @Value("${domain_name:http://forward-front-forward-v1.projectk8s.tsign.cn/}")
    private String domainName;

    private static final String SINGLE_TITLE = "点击查看详情";
    private static final String BIZ_DEMAND_RECEIVED_MSG = "### %s  \n  **%s**接收了您提交的业务需求 **%s**，预期上线时间为 **%s**";
    private static final String BIZ_DEMAND_REJECT_MSG = "### %s  \n  **%s**驳回了您提交的业务需求 **%s**，驳回理由是 **%s**";
    private static final String BIZ_DEMAND_STATUS_CHANGE_MSG = "### %s  \n  您提交的业务需求 **%s** 状态已变为 **%s**，项目发布时间为 **%s**";
    private static final String BIZ_DEMAND_TO_RECEIVE_MSG = "### %s  \n  您收到了**%s**提交的业务需求 **%s**";
    private static final String BIZ_DEMAND_INVALID_MSG = "### %s  \n  **%s**作废了业务需求 **%s**";
    private static final String COMMENT_MSG = "### %s  \n  **%s**评论了%s **%s**  \n  > %s";
    private static final String PARAM = "%s?id=%d&type=check";

    @Override
    public void bizDemandReceivedMsg(Long bizDemandId, String operator, String receiver, String name, String planReleaseDate) {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_FEEDBACK.getText();
        String markdown = String.format(BIZ_DEMAND_RECEIVED_MSG, title, operator, name, planReleaseDate);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_EDIT.getText(), bizDemandId);
        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle(SINGLE_TITLE)
                .singleUrl(singleUrl)
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
    }

    @Override
    public void bizDemandRejectMsg(Long bizDemandId, String operator, String receiver, String name, String rejectReason) {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_FEEDBACK.getText();
        String markdown = String.format(BIZ_DEMAND_REJECT_MSG, title, operator, name, rejectReason);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_EDIT.getText(), bizDemandId);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle(SINGLE_TITLE)
                .singleUrl(singleUrl)
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
    }

    @Override
    public void bizDemandStatusChangeMsg(Long bizDemandId, String receiver, String name, String status, Date projectEndDate) {
        String date = DateUtil.getDate(projectEndDate);

        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_STATUS_CHANGE.getText();
        String markdown = String.format(BIZ_DEMAND_STATUS_CHANGE_MSG, title, name, status, date);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_EDIT.getText(), bizDemandId);
        log.info("singleUrl:" + singleUrl);
        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle(SINGLE_TITLE)
                .singleUrl(singleUrl)
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
    }

    @Override
    public void bizDemandToReceiveMsg(Long bizDemandId, String operator, String receiver, String name) {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_RECEIVE.getText();
        String markdown = String.format(BIZ_DEMAND_TO_RECEIVE_MSG, title, operator, name);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_EDIT.getText(), bizDemandId);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle(SINGLE_TITLE)
                .singleUrl(singleUrl)
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
    }

    @Override
    public void bizDemandInvalidMsg(Long bizDemandId, String operator, String receiver, String name) {
        List<String> receivers = Lists.newArrayList(receiver);
        String title = MessageTitleEnum.BIZDEMAND_INVALID.getText();
        String markdown = String.format(BIZ_DEMAND_INVALID_MSG, title, operator, name);
        String singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_EDIT.getText(), bizDemandId);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle(SINGLE_TITLE)
                .singleUrl(singleUrl)
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
    }

    @Override
    public void commentMsg(Long mainId, String operator, List<String> receivers, String type, String name, String content) {
        String title = type + MessageTitleEnum.COMMENT.getText();
        String markdown = String.format(COMMENT_MSG, title, operator, type, name, content);
        String singleUrl;
        if (CommentTypeEnum.PROJECT.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.PROJECT_EDIT.getText(), mainId);
        } else if (CommentTypeEnum.PRODUCT_DEMAND.getText().equals(type)) {
            singleUrl = domainName + String.format(PARAM, TabEnum.PRODUCT_EDIT.getText(), mainId);
        } else {
            singleUrl = domainName + String.format(PARAM, TabEnum.BUSINESS_EDIT.getText(), mainId);
        }
        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleTitle(SINGLE_TITLE)
                .singleUrl(singleUrl)
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
    }
}
