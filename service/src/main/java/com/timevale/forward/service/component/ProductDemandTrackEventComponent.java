package com.timevale.forward.service.component;

import java.util.List;

public interface ProductDemandTrackEventComponent {

    /**
     *
     * @param trackEventId 项目id
     */
    void update(Long productDemandId,Long trackEventId);

    /**
     * 新增项目-产品需求
     *
     * @param trackEventIds 新增项目-产品需求id
     */
    void batchInsert(Long productDemandId, List<Long> trackEventIds);

}
