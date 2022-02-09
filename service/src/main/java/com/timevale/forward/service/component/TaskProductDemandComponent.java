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
     * @param taskIds taskIds
     * @param productDemandId productDemandId
     */
    void update(List<Long> taskIds, Long productDemandId);
    /**
     * 任务对应的产品需求状态全部变更为已暂停/作废时时，取消关联任务
     *
     * @param productDemandId productDemandId
     */
    void unLinkIfProductDemandStatusAllChange(Long productDemandId);

}
