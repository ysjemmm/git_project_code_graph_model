package com.timevale.forward.service.component;

import java.util.List;

public interface ProductBizDemandComponent {

    /**
     *
     * @param bizDemandId 项目id
     */
    void update(Long productDemandId,Long bizDemandId,boolean updatePublishDate);

    /**
     * 新增项目-产品需求
     *
     * @param bizDemandIds 新增项目-产品需求id
     */
    void batchInsert(Long productDemandId, List<Long> bizDemandIds,boolean updatePublishDate);

}
