package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandListDO;

import java.util.List;

public interface ProductDemandComponent {
    /**
     *
     * @param productDemandListCondition 查询条件
     * @return 列表
     */
    List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition);

}
