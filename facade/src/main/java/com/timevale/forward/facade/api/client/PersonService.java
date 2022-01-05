package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface PersonService {
    /**
     * 修改
     *
     * @param recipientAddReq 抄送人信息
     * @return 数量
     */
    BaseResult<Boolean> addRecipients(RecipientAddReq recipientAddReq);
}
