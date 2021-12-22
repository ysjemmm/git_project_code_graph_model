package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import org.apache.ibatis.annotations.Param;

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
     * @param productBizDemandDOList 产品业务需求ist
     * @return int
     */
    int inserts(List<ProductBizDemandDO> productBizDemandDOList);

    /**
     * 批量更新
     *
     * @param productBizDemandDOList 产品业务需求list
     * @return int
     */
    int updates(List<ProductBizDemandDO> productBizDemandDOList);

    /**
     * 逻辑删除
     *
     * @param bizDemandId 业务需求id
     * @return int
     */
    int deleteByBizDemandId(@Param("bizDemandId") Long bizDemandId);

}
