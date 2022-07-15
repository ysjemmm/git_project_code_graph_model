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
     * @param productDemandDO 产品需求
     */
    void update(ProductDemandDO productDemandDO);

    /**
     *
     * @param projectId 项目id
     * @param status 项目状态
     */
    void updateProductDemandStatus(Long projectId,Integer status);

    /**
     *
     * @param productDemandIds 产品需求id
     * @param invalid 产品需求与关联业务需求取消关联,
     */
    void updateBizDemandStatusAsProductStatusChange(List<Long> productDemandIds,boolean invalid);

    /**
     *
     * @param bizDemandId bizDemandId
     * @param productDemandId productDemandId
     */
    void updateBizDemandStatusWhenUnlink(Long bizDemandId,Long productDemandId);

    /**
     *
     * @param productStatus productStatus 产品需求状态
     * @param bizDemandId bizDemandId
     * @param bizDemand bizDemand
     */
    Integer updateDemandStatus(Integer productStatus, Long bizDemandId,boolean bizDemand);

}
