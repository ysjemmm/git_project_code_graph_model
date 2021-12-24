package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import org.apache.ibatis.annotations.Param;

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
     * 选择id查询
     *
     * @param id id
     * @return DO
     */
    ProductDemandDO selectById(Long id);


    /**
     * 批量查询产品需求
     *
     * @param productDemandIdList 产品需求id列表
     * @return list
     */
    List<ProductDemandDO> selectByIdList(@Param("productDemandIdList") List<Long> productDemandIdList);

    /**
     * 选择通过业务需求id
     *
     * @param bizDemandId 业务需求id
     * @return {@link List<BizDemandLinkProductDemandListDO> }
     */
    List<BizDemandLinkProductDemandListDO> selectByBizDemandId(@Param("bizDemandId") Long bizDemandId);

    /**
     * 业务需求关联产品查询
     *
     * @param bizDemandLinkProductDemandListCondition 业务需求链接产品需求列表条件
     * @return list
     */
    List<BizDemandLinkProductDemandListDO> selectListOfBizDemandLink(BizDemandLinkProductDemandListCondition bizDemandLinkProductDemandListCondition);

    /**
     * 查询产品需求
     *
     * @param productDemandListCondition 产品需求Id列表
     * @return list
     */
    List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition);

    /**
     *
     * @param projectId 查询条件
     * @return 项目产品需求清单
     */
    List<ProductDemandListDO> projectProductList(@Param("projectId") Long projectId);


    /**
     * 新增单条产品需求
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int update(ProductDemandDO productDemandDO);
}
