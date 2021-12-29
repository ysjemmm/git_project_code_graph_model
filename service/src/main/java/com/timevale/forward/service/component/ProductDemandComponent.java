package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;

import java.util.List;

public interface ProductDemandComponent {
    /**
     *
     * @param productDemandListCondition 查询条件
     * @return 列表
     */
    List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition);

    /**
     *
     * @param id 查询条件
     * @return 详情
     */
    ProductDemandDetailVO get(Long id);

    /**
     *
     * @param productDemandDO 项目id
     */
    void update(ProductDemandDO productDemandDO);

    /**
     *
     * @param productDemandIds productDemandIds
     * @param needUpdateStatusWhenLinkOne  业务需求和产品需求 1-1时需要更新的该状态
     */
    void updateBizDemandStatusIfNecessary(List<Long> productDemandIds,Integer needUpdateStatusWhenLinkOne);

}
