package com.timevale.forward.service.component;

// 引入必要的包
import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.entity.ProductDemandGroupItemListDO;

import java.util.List;

/**
 * 产品-分组关系表 组件接口
 */
public interface ProductDemandGroupItemComponent {
    // 删除某分组下产品需求
    void deleteByGroupId(Long groupId);
    // 查询某分组下产品需求
    List<ProductDemandGroupItemListDO> listProductDemand(ProductDemandGroupListCondition productDemandGroupListCondition);
} 