package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;
import java.util.Map;

/**
 * Devops发布火车服务接口
 *
 * @author dijiu
 * @date 2025/9/17
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface DevopsTrainTestCaseService {

    /**
     * 根据发布火车ID获取测试用例版本和轮次级联信息
     *
     * @param trainId 发布火车ID
     * @return 测试用例版本和轮次级联信息
     */
    BaseResult<Map<String, Object>> getTestCaseCascadeInfoByTrainId(Integer trainId);
}
