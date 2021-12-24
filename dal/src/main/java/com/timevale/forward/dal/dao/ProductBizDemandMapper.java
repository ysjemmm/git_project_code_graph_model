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
     * @param productBizDemandDOList 产品业务需求list
     * @return int
     */
    int inserts(@Param("productBizDemandDOList") List<ProductBizDemandDO> productBizDemandDOList);

    /**
     * 批量更新
     *
     * @param list        产品业务需求id list
     * @param isDeleted   是否删除
     * @param modifyMan   修改人
     * @param modifyManId 修改人身份证
     * @return int
     */
    int updates(@Param("list") List<Long> list, @Param("isDeleted")Boolean isDeleted, @Param("modifyMan")String modifyMan, @Param("modifyManId") String modifyManId);

    /**
     * 逻辑删除
     *
     * @param bizDemandId 业务需求id
     * @return int
     */
    int deleteByBizDemandId(@Param("bizDemandId") Long bizDemandId, @Param("modifyMan")String modifyMan, @Param("modifyManId") String modifyManId);


    /**
     * 删除
     *
     * @param productBizDemandDO 产品业务需求DO
     * @return int
     */
    int delete(ProductBizDemandDO productBizDemandDO);

}
