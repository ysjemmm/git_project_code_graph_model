package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.DynamicBizDemandGroupList;
import com.timevale.forward.facade.api.query.DynamicBugOfflineGroupList;
import com.timevale.forward.facade.api.query.DynamicProductDemandGroupList;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.BugOfflineVO;
import com.timevale.forward.facade.api.result.DemandGroupNodeVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

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
    BaseResult<PageQueryResult<ProductDemandVO>> getProductDemandList(DynamicProductDemandGroupList dynamicGroupQueryList);

    /**
     * 获取业务需求分组树
     * @param dynamicGroupQueryList
     * @return
     */
    BaseResult<List<DemandGroupNodeVO>> getBizDemandsGroupTree(DynamicBizDemandGroupList dynamicGroupQueryList);

    /**
     * 获取业务需求列表
     * @param dynamicGroupQueryList
     * @return
     */
    BaseResult<PageQueryResult<BizDemandVO>> getBizDemandList(DynamicBizDemandGroupList dynamicGroupQueryList);

    /**
     * 获取线下Bug分组树
     * @param dynamicGroupQueryList
     * @return
     */
    BaseResult<List<DemandGroupNodeVO>> getBugOfflineGroupTree(DynamicBugOfflineGroupList dynamicGroupQueryList);

    /**
     * 获取线下Bug分组列表
     * @param dynamicGroupQueryList
     * @return
     */
    BaseResult<PageQueryResult<BugOfflineVO>> getBugOfflineList(DynamicBugOfflineGroupList dynamicGroupQueryList);
}
