package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductCustomDemandCondition;
import com.timevale.forward.dal.entity.ProductCustomDemandDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/16 14:06
 */
public interface ProductCustomDemandMapper {

    /**
     * 查询客户客户需求关联
     *
     * @param customDemandCondition 客户客户需求查询条件
     * @return 列表
     */
    List<ProductCustomDemandDO> select(ProductCustomDemandCondition customDemandCondition);

    /**
     * 更新单条客户需求
     *
     * @param productCustomDemandDO 客户需求DO
     * @return int
     */
    int update(ProductCustomDemandDO productCustomDemandDO);


    /**
     * 新增项目-客户需求
     *
     * @param productCustomDemandDOList 新增项目-客户需求
     * @return int
     */
    int batchInsert(List<ProductCustomDemandDO> productCustomDemandDOList);

    /**
     * 查询产品业务需求关联
     *
     * @param productDemandIds 产品业务需求查询条件
     * @return 列表
     */
    List<ProductCustomDemandDO> getByProductDemandIds(@Param("productDemandIds") List<Long> productDemandIds);

    /**
     * 查询产品业务需求关联
     * @param customDemandId 产品业务需求查询条件
     *
     * @return 列表
     */
    List<ProductCustomDemandDO> getByCustomDemandId(@Param("customDemandId") Long customDemandId);


    List<ProductCustomDemandDO> selectByProductDemandIds(@Param("productDemandIds") List<Long> productDemandIds);

}
