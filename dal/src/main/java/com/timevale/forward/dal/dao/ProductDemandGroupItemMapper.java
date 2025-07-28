package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.entity.ProductDemandGroupItemDO;
import com.timevale.forward.dal.entity.ProductDemandGroupItemListDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 产品-分组关系表Mapper接口
 * @author by qiyuan
 * @date 2025/07/15 10:42
 */
public interface ProductDemandGroupItemMapper {

    /**
     * 根据条件查询分组的产品需求
     *
     * @param productDemandGroupListCondition 产品需求分组查询条件
     * @return list
     */
    List<ProductDemandGroupItemListDO> listProductDemand(ProductDemandGroupListCondition productDemandGroupListCondition);

    /**
     * 根据条件查询待规划的产品需求
     *
     * @param productDemandGroupListCondition 产品需求查询条件
     * @return list
     */
    List<ProductDemandListDO> listProductDemandBacklog(ProductDemandGroupListCondition productDemandGroupListCondition);

    /**
     * 删除产品需求分组和产品需求关系
     *
     * @param groupId     分组id
     * @param modifyManId 修改人id
     * @param modifyMan   修改人
     */
    void deleteByGroupId(@Param("groupId") Long groupId, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);

    /**
     * 根据主键id获取产品需求分组和产品需求关系
     *
     * @param id 主键id
     * @return 产品需求分组和产品需求关系
     */
    ProductDemandGroupItemDO get(@Param("id") Long id);

    /**
     * 根据分组id获取产品需求关系列表
     *
     * @param groupId 分组id
     * @return 产品需求分组和产品需求关系列表
     */
    List<ProductDemandGroupItemDO> getByGroupId(@Param("groupId") Long groupId);

    /**
     * 根据业务域和主键id获取产品需求分组和产品需求关系
     *
     * @param bizDomainId 业务域id
     * @param id 主键id
     * @return 产品需求分组和产品需求关系
     */
    ProductDemandGroupItemDO getByBizDomainIdAndId(@Param("bizDomainId") Long bizDomainId, @Param("id") Long id);


    /**
     * 根据分组id和主键id获取产品需求分组和产品需求关系
     *
     * @param groupId 分组id
     * @param id 主键id
     * @return 产品需求分组和产品需求关系
     */
    ProductDemandGroupItemDO getByGroupIdAndId(@Param("groupId") Long groupId, @Param("id") Long id);

    /**
     * 根据分组id和产品需求id获取产品需求分组和产品需求关系
     *
     * @param demandId 产品需求id
     * @param groupId 分组id
     * @return 产品需求分组和产品需求关系
     */
    ProductDemandGroupItemDO getByGroupIdAndDemandId(@Param("groupId") Long groupId, @Param("demandId") Long demandId);

    /**
     * 查询产品需求id是否被关联
     *
     * @param productDemandId 产品需求id
     * @return 产品需求分组和产品需求关系
     */
    Integer countByProductDemandId(@Param("productDemandId") Long productDemandId);

    /**
     * 获取当前位置前一个分组和产品需求关系
     *
     * @param productDemandGroupId 分组id
     * @param position    当前位置
     * @return 产品需求分组和产品需求关系
     */
    ProductDemandGroupItemDO getPreByPosition(@Param("productDemandGroupId") Long productDemandGroupId, @Param("position") Double position);

    /**
     * 获取当前位置后一个分组和产品需求关系
     *
     * @param productDemandGroupId 分组id
     * @param position    当前位置
     * @return 产品需求分组和产品需求关系
     */
    ProductDemandGroupItemDO getNextByPosition(@Param("productDemandGroupId") Long productDemandGroupId, @Param("position") Double position);

    /**
     * 更新产品需求分组和产品需求关系的位置
     *
     * @param  productDemandGroupItemDO productDemandGroupItemDO
     * @return 影响行数
     */
    int updatePosition(ProductDemandGroupItemDO productDemandGroupItemDO);

    /**
     * 新增一条产品需求分组产品需求关系
     *
     * @param productDemandGroupItemDO productDemandGroupItemDO
     * @return 影响行数
     */
    int insert(ProductDemandGroupItemDO productDemandGroupItemDO);

    /**
     * 删除产品需求分组产品需求关系
     *
     * @param id          主键id
     * @param modifyManId 修改人id
     * @param modifyMan   修改人
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);

} 