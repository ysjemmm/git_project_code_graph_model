package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProductDemandDescRecordDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @author jingchun
* description 针对表【product_demand_desc_record(产品需求描述记录表)】的数据库操作Mapper
* createDate 2022-07-01 14:25:57
* Entity com.timevale.forward.dal.entity.ProductDemandDescRecordDO
*/
public interface ProductDemandDescRecordMapper {

    List<ProductDemandDescRecordDO> listByProductDemandId(@Param("productDemandId") Long productDemandId);

    ProductDemandDescRecordDO getLatestByProductDemandId(@Param("productDemandId") Long productDemandId);

    Integer countByProductDemandId(@Param("productDemandId") Long productDemandId);

    void insert(ProductDemandDescRecordDO productDemandDescRecordDO);
}




