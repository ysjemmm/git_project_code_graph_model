package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProductDemandOwnerTimeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 产品需求资源人天分配 Mapper
 *
 * @author kiro
 * @date 2026-02-24
 */
@Mapper
public interface ProductDemandOwnerTimeMapper {

    /**
     * 根据需求ID列表查询资源分配
     */
    List<ProductDemandOwnerTimeDO> listByProductDemandIds(@Param("productDemandIds") Collection<Long> productDemandIds);

    /**
     * 根据分组ID查询资源分配
     */
    List<ProductDemandOwnerTimeDO> listByGroupId(@Param("groupId") Long groupId);

    /**
     * 根据负责人ID查询资源分配（人员视角）
     */
    List<ProductDemandOwnerTimeDO> listByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 根据分组ID + 负责人ID列表查询（人员视角 + 分组过滤）
     */
    List<ProductDemandOwnerTimeDO> listByGroupIdAndOwnerIds(
            @Param("groupId") Long groupId,
            @Param("ownerIds") Collection<String> ownerIds
    );

    /**
     * 批量插入
     */
    int batchInsert(
            @Param("list") List<ProductDemandOwnerTimeDO> list,
            @Param("operatorId") String operatorId,
            @Param("operator") String operator
    );

    /**
     * 逻辑删除：按需求ID + 资源类型
     */
    int deleteByDemandIdAndType(
            @Param("productDemandId") Long productDemandId,
            @Param("resourceType") String resourceType,
            @Param("operatorId") String operatorId,
            @Param("operator") String operator
    );

    /**
     * 逻辑删除：按需求ID（删除该需求所有资源分配）
     */
    int deleteByDemandId(
            @Param("productDemandId") Long productDemandId,
            @Param("operatorId") String operatorId,
            @Param("operator") String operator
    );

    /**
     * 逻辑删除：按需求ID列表
     */
    int deleteByDemandIds(
            @Param("productDemandIds") Collection<Long> productDemandIds,
            @Param("operatorId") String operatorId,
            @Param("operator") String operator
    );

    /**
     * 按分组ID列表汇总各资源类型人天
     */
    List<Map<String, Object>> sumByGroupIds(@Param("groupIds") Collection<Long> groupIds);
}
