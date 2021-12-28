package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.ProductBizDemandDO;

import java.util.List;

public interface ProductBizDemandComponent {

    /**
     *
     * @param productBizDemandDO 项目id
     */
    void update(ProductBizDemandDO productBizDemandDO);

    /**
     * 新增项目-产品需求
     *
     * @param bizDemandIds 新增项目-产品需求id
     */
    void batchInsert(Long productDemandId, List<Long> bizDemandIds);

}
