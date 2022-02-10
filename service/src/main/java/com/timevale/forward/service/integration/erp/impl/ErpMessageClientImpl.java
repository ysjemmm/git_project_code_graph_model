package com.timevale.forward.service.integration.erp.impl;

import com.timevale.erp.message.service.api.ErpMsgService;
import com.timevale.erp.message.service.model.ErpActionCardMsgSendInput;
import com.timevale.erp.message.service.model.ErpMarkdownMsgSendInput;
import com.timevale.erp.message.service.model.Receiver;
import com.timevale.erp.message.service.result.ErpResult;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.stream.Collectors;

/**
 * @author yuankai
 * @date 2021/12/16 15:55
 */
@Slf4j
@Service
public class ErpMessageClientImpl implements ErpMessageClient {

    @Resource
    private ErpMsgService erpMsgService;

    @Override
    public ErpResult sendActionCardMsg(ActionCardMsg actionCardMsg) {
        final ErpActionCardMsgSendInput input = new ErpActionCardMsgSendInput();
        input.setReceivers(actionCardMsg.getReceivers());
        input.setTitle(actionCardMsg.getTitle());
        input.setMarkdown(actionCardMsg.getMarkdown());
        input.setSingleTitle(actionCardMsg.getSingleTitle());
        input.setSingleUrl(actionCardMsg.getSingleUrl());

        ErpResult erpResult = erpMsgService.sendActionCardMsg(input);
        if (erpResult.isSuccess()) {
            return erpResult;
        }
        log.error("[erpMessage]调用钉钉通知接口失败  error: " + erpResult.getMessage() + " 发送通知信息：" + actionCardMsg);
        throw new BaseBizRuntimeException("调用钉钉通知接口失败! " + actionCardMsg);
    }

    @Override
    public ErpResult sendMarkdownMsg(MarkdownMsg markdownMsg) {
        if (CollectionUtils.isEmpty(markdownMsg.getReceivers())) {
            throw new BaseBizRuntimeException("消息接收人不能为空");
        }
        final ErpMarkdownMsgSendInput input = new ErpMarkdownMsgSendInput();
        input.setReceivers(markdownMsg.getReceivers().stream().map(n -> {
            final Receiver receiver = new Receiver();
            receiver.setAccountId(n);
            return receiver;
        }).collect(Collectors.toList()));
        input.setBizObtTitle(markdownMsg.getTitle());
        input.setBizObt(markdownMsg.getContent());

        ErpResult erpResult = erpMsgService.sendDingMarkdownMsg(input);
        if (!erpResult.isSuccess()) {
            log.error("[erpMessage]调用钉钉通知接口失败  error: " + erpResult.getMessage() + " 发送通知信息：" + markdownMsg);
        }
        return erpResult;
    }
}
