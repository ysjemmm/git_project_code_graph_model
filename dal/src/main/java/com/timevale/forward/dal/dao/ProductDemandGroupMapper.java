package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
import com.timevale.forward.dal.entity.ProductDemandGroupDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 产品需求分组表Mapper
 *
 * @author by qiyuan
 * @date 2025/07/14 14:42
 */
public interface ProductDemandGroupMapper {

    /**
     * 查询产品需求分组
     *
     * @param productDemandGroupListCondition 产品需求分组查询条件
     * @return list
     */
    List<ProductDemandGroupDO> list(ProductDemandGroupListCondition productDemandGroupListCondition);

    /**
     * 新增一条产品需求分组信息
     *
     * @param productDemandGroupDO 产品需求分组DO
     * @return 影响行数
     */
    int insert(ProductDemandGroupDO productDemandGroupDO);

    /**
     * 更新产品需求分组信息
     *
     * @param productDemandGroupDO 产品需求分组DO
     * @return 影响行数
     */
    int update(ProductDemandGroupDO productDemandGroupDO);

    /**
     * 更新产品需求分组位置
     *
     * @param  productDemandGroupDO 产品需求分组DO
     * @return 影响行数
     */
    int updatePosition(ProductDemandGroupDO productDemandGroupDO);

    /**
     * 删除产品需求分组信息
     *
     * @param id          主键id
     * @param modifyManId 修改人id
     * @param modifyMan   修改人
     * @return 影响行数
     */
    int delete(@Param("id") Long id, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);

    /**
     * 根据主键id获取产品需求分组信息
     *
     * @param id 主键id
     * @return 产品需求分组DO
     */
    ProductDemandGroupDO get(@Param("id") Long id);

    /**
     * 根据业务域id和id查询产品需求分组
     *
     * @param bizDomainId 业务域id
     * @param id 主键id
     * @return 产品需求分组DO
     */
    ProductDemandGroupDO getByIdAndBizDomainId(@Param("bizDomainId") Long bizDomainId, @Param("id") Long id);

    /**
     * 根据业务域id和名称查询产品需求分组
     *
     * @param bizDomainId 业务域id
     * @param name        分组名称
     * @return 产品需求分组DO
     */
    ProductDemandGroupDO getByBizDomainIdAndName(@Param("bizDomainId") Long bizDomainId, @Param("name") String name);

    /**
     * 获取当前位置前一个分组
     *
     * @param bizDomainId 业务域id
     * @param position    当前位置
     * @return 产品需求分组DO
     */
    ProductDemandGroupDO getPreByPosition(@Param("bizDomainId") Long bizDomainId, @Param("position") Double position);

    /**
     * 获取当前位置后一个分组
     *
     * @param bizDomainId 业务域id
     * @param position    当前位置
     * @return 产品需求分组DO
     */
    ProductDemandGroupDO getNextByPosition(@Param("bizDomainId") Long bizDomainId, @Param("position") Double position);

    /**
     * 统计个数
     *
     * @param projectId 项目id
     * @return {@link Long}
     */
    Long countProject(@Param("projectId") Long projectId);

    /**
     * 根据项目id获取产品需求分组
     *
     * @param projectId 项目id
     * @return 产品需求分组DO
     */
    ProductDemandGroupDO getByProjectId(@Param("projectId") Long projectId);

    /**
     * 取消关联项目
     *
     * @param id 主键id
     */
    void removeProject(@Param("id") Long id);
} 