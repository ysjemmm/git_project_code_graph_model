package com.timevale.forward.service.component;

import java.util.List;

public interface ProjectProductDemandComponent {
    /**
     *
     * @param projectId projectId
     * @param productDemandId productDemandId
     */
    void update(Long projectId,Long productDemandId);

    /**
     * 新增项目-产品需求
     *
     * @param projectId 新增项目-产品需求id
     */
    void batchInsert(Long projectId, List<Long> productDemandIds);


}
