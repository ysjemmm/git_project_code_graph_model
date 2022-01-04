package com.timevale.forward.service.component;

import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;

import java.util.List;

public interface ProductDemandComponent {
    /**
     *
     * @param productDemandListCondition 查询条件
     * @return 列表
     */
    List<ProductDemandListDO> list(ProductDemandListCondition productDemandListCondition);

    /**
     *
     * @param id 查询条件
     * @return 详情
     */
    ProductDemandDetailVO get(Long id);

    /**
     *
     * @param productDemandDO 项目id
     */
    void update(ProductDemandDO productDemandDO);

    /**
     *
     * @param productDemandIds 产品需求id
     * @param bizProductDemandUnLink 产品需求与关联业务需求取消关联,
     */
    void updateBizDemandStatusAsProductStatusChange(List<Long> productDemandIds,boolean bizProductDemandUnLink);

    /**
     *
     * @param productDemandId 产品需求id
     * @param bizDemandId 业务需求id
     * @param bizProductDemandUnLink 业务需求关联或者取消关联产品需求
     */
    void updateBizDemandStatusAsWhenLinkOrUnlink(Long productDemandId,Long bizDemandId,boolean bizProductDemandUnLink);

}
