package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandDescFlowMapper;
import com.timevale.forward.dal.entity.ProductDemandDescFlowDO;
import com.timevale.forward.facade.api.client.ProductDemandDescFlowService;
import com.timevale.forward.facade.api.result.ProductDemandDescFlowVO;
import com.timevale.forward.service.copy.ProductDemandDescFlowCopier;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * 产品需求变更流程接口
 *
 * @author jingchun
 * create on 2022/7/4
 */
@Slf4j
@RestService
public class ProductDemandDescFlowServiceImpl implements ProductDemandDescFlowService {

    @Resource
    private ProductDemandDescFlowMapper productDemandDescFlowMapper;

    @Override
    public BaseResult<ProductDemandDescFlowVO> getLatestDescFlow(Long productDemandId) {
        ProductDemandDescFlowDO flow = productDemandDescFlowMapper.getLastByProductDemandId(productDemandId);
        return BaseResult.success(ProductDemandDescFlowCopier.INSTANCE.convert(flow));
    }
}
