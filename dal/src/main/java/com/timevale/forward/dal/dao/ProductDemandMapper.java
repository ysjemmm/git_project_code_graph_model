package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProductDemandDO;

import java.util.List;

public interface ProductDemandMapper {
    /**
     * 新增单条产品需求
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int insert(ProductDemandDO productDemandDO);


    /**
     * 批量查询产品需求
     *
     * @param list 产品需求Id列表
     * @return list
     */
    List<ProductDemandDO> select(List<Long> list);
}
