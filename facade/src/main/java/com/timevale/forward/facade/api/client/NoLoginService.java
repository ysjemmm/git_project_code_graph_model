package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @auther: yuhua
 * @date: 2025/7/28 10:55
 * @description: 免登服务
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface NoLoginService {

    /**
     * 获取token
     * @param dateStr
     * @param userId
     * @return
     */
    BaseResult<String> getToken(String dateStr, String userId);
}
