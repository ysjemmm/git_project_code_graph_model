package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.entity.ProductBizDemandDO;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 14:06
 */
public interface ProductBizDemandMapper {

    /**
     * 查询产品业务需求关联
     *
     * @param productBizDemandCondition 产品业务需求查询条件
     * @return 列表
     */
    List<ProductBizDemandDO> select(ProductBizDemandCondition productBizDemandCondition);

    /**
     * 批量插入
     *
     * @param list 列表
     * @return int
     */
    int inserts(List<ProductBizDemandDO> list);

    /**
     * 批量更新
     *
     * @param list 列表
     * @return int
     */
    int updates(List<ProductBizDemandDO> list);

    /**
     * 批量删除
     *
     * @param list 列表
     * @return int
     */
    int delete(List<ProductBizDemandDO> list);

}
