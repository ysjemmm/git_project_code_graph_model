package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
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
     * 根据项目id查询
     * @param projectId 项目d
     * @return 产品需求列表
     */
    List<ProductDemandDO> selectByProjectId(Long projectId);

    /**
     * 批量查询产品需求
     *
     * @param productDemandIdList 产品需求id列表
     * @return list
     */
    List<ProductDemandDO> selectByIdList(@Param("productDemandIdList") Collection<Long> productDemandIdList);

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
    List<ProductDemandListDO> linkProductDemandList(@Param("projectId") Long projectId);


    /**
     * 更新单条产品需求
     *
     * @param productDemandDO 产品需求DO
     * @return int
     */
    int update(ProductDemandDO productDemandDO);

    /**
     * 更新负责人 by id列表
     *
     * @param idList  id列表
     * @param owner   负责人
     * @param ownerId 负责人id
     */
    int updateOwner(@Param("idList") List<Long>idList, @Param("owner") String owner, @Param("ownerId") String ownerId);

    /**
     * 查询
     * @param id id
     * @return 产品需求DO
     */
    ProductDemandDO get(@Param("id") Long id);


    /**
     * 更新产品需求
     *
     * @param ids 产品需求DO
     * @return int
     */
    int updateByIds(@Param("ids") List<Long> ids,@Param("status") Integer status,@Param("retainModifyDate") boolean retainModifyDate);

    /**
     * 查询
     * @param name name
     * @return 产品需求DO
     */
    ProductDemandDO getByName(@Param("name") String name);

    /**
     *
     * @param customDemandId customDemandId
     * @return 项目产品需求清单
     */
    List<ProductDemandListDO> linkProductDemandInCustomDemand(@Param("customDemandId") Long customDemandId);

    /**
     * 查询产品需求
     *
     * @param ownerId ownerId
     * @return list
     */
    List<ProductDemandDO> getByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 通过产品线ID获取产品需求
     *
     * @param productLineId 产品线ID
     * @return
     */
    @Select("select * from product_demand where product_line_id=#{productLineId} AND is_deleted=false")
    List<ProductDemandDO> getByProductLineId(@Param("productLineId") Long productLineId);

}
