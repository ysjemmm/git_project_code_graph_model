package com.timevale.forward.service.integration.erp;

import com.timevale.erp.message.service.result.ErpResult;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.integration.erp.model.MarkdownMsg;

/**
 * 钉钉消息
 *
 * @author yuankai
 * @date 2021/12/16 15:51
 */
public interface ErpMessageClient {
    /**
     * 发送钉钉ActionCard 消息
     *
     * @param actionCardMsg 内容
     * @return 结果
     */
    ErpResult sendActionCardMsg(ActionCardMsg actionCardMsg);

    /**
     * 发送markdown 消息
     * @param markdownMsg 内容
     * @return 结果
     */
    ErpResult sendMarkdownMsg(MarkdownMsg markdownMsg);
}
