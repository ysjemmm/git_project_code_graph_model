package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandDescFlowMapper;
import com.timevale.forward.dal.entity.ProductDemandDescFlowDO;
import com.timevale.forward.facade.api.client.ProductDemandDescFlowService;
import com.timevale.forward.facade.api.result.ProductDemandDescFlowVO;
import com.timevale.forward.model.enums.FlowStatusEnum;
import com.timevale.forward.service.copy.ProductDemandDescFlowCopier;
import com.timevale.mandarin.base.util.AssertUtil;
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

    @Override
    public BaseResult<Boolean> withdrawProductDemandDescFlow(Long productDemandId) {
        ProductDemandDescFlowDO flow = productDemandDescFlowMapper.getLastByProductDemandId(productDemandId);
        AssertUtil.notNull(flow, "该产品需求不存在流程变更记录，无法撤回流程");
        AssertUtil.checkState(FlowStatusEnum.AUDITING.getCode().equals(flow.getStatus()), "该审批流程处于" +
                FlowStatusEnum.getTextByCode(flow.getStatus()) + "状态，无法撤回");
        // TODO jingchun 发起撤回
        return BaseResult.success(true);
    }
}
