package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.result.ProductDemandDescFlowVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author jingchun
 * create on 2022/7/1
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProductDemandDescFlowService {

    BaseResult<ProductDemandDescFlowVO> getLatestDescFlow(Long productDemandId);

}
