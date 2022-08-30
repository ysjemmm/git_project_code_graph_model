package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.ProductDemandDescFlowDO;
import org.apache.ibatis.annotations.Param;

/**
* @author jingchun
* description 针对表【product_demand_desc_flow(产品需求描述审批流程表)】的数据库操作Mapper
* createDate 2022-07-01 14:23:48
* Entity com.timevale.forward.dal.entity.ProductDemandDescFlow
*/
public interface ProductDemandDescFlowMapper {

    ProductDemandDescFlowDO getLastByProductDemandId(@Param("productDemandId") Long productDemandId);

    void insert(ProductDemandDescFlowDO flow);

    ProductDemandDescFlowDO getAuditingByFlowId(@Param("flowId") String flowId);

    void update(ProductDemandDescFlowDO auditingFlow);
}




