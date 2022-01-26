package com.timevale.forward.service.component;

import java.util.List;

public interface TaskProductDemandComponent {

    /**
     * 新增任务-产品需求
     * @param taskId taskId
     * @param bizDemandIds bizDemandIds
     */
    void batchInsert(Long taskId, List<Long> bizDemandIds);

    /**
     * 新增任务-产品需求
     * @param taskId taskId
     * @param productDemandId productDemandId
     */
    void update(Long taskId, Long productDemandId);

}
