package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.DynamicProductDemandGroupList;
import com.timevale.forward.facade.api.result.DemandGroupNodeVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/8/13 15:11
 * @description:
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface DynamicGroupService {

    /**
     * 获取产品需求分组树
     * @param dynamicGroupQueryList
     * @return
     */
    BaseResult<List<DemandGroupNodeVO>> getProductDemandsGroupTree(DynamicProductDemandGroupList dynamicGroupQueryList);

    /**
     * 获取产品需求列表
     * @param dynamicGroupQueryList
     * @return
     */
    BaseResult<QueryResultVO<ProductDemandVO>> getProductDemandList(DynamicProductDemandGroupList dynamicGroupQueryList);
}
