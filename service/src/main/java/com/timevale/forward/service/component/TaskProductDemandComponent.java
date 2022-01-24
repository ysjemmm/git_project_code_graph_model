package com.timevale.forward.service.component;

import java.util.List;

public interface TaskProductDemandComponent {

    /**
     * 新增项目-产品需求
     *
     * @param bizDemandIds 新增项目-产品需求id
     */
    void batchInsert(Long productDemandId, List<Long> bizDemandIds);

}
