package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProductDemandIdReq;
import com.timevale.forward.facade.api.result.ProductDemandDescFlowVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author jingchun
 * create on 2022/7/1
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProductDemandDescFlowService {

    /**
     * 查询最近一次发起的产品需求变更流程
     * @param productDemandId 产品需求id
     * @return 流程记录
     */
    BaseResult<ProductDemandDescFlowVO> getLatestDescFlow(Long productDemandId);

    /**
     * 撤回最近一次发起的产品需求变更流程
     * @param productDemandId 产品需求id
     * @return 是否成功
     */
    BaseResult<Boolean> withdrawProductDemandDescFlow(ProductDemandIdReq productDemandId);


}
