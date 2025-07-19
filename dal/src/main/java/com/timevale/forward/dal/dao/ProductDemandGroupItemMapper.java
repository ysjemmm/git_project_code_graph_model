package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.condition.ProductDemandGroupListCondition;
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
     * @param groupId 分组id
     * @param modifyManId 修改人id
     * @param modifyMan 修改人
     * @return 影响行数
     */
    int deleteByGroupId(@Param("groupId") Long groupId, @Param("modifyManId") String modifyManId, @Param("modifyMan") String modifyMan);


} 