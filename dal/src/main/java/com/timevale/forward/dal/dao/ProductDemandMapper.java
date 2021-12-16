package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProductDemandDO;

public interface ProductDemandMapper {
    /**
     * 新增单条产品需求
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int insert(ProductDemandDO productDemandDO);


}
