package com.timevale.forward.service.component;

import java.util.List;

public interface ProductCustomDemandComponent {


    /**
     *
     * @param productDemandId productDemandId
     * @param customDemandId customDemandId
     */
    void update(Long productDemandId,Long customDemandId);

    /**
     *
     * @param productDemandId productDemandId
     * @param customDemandIds customDemandIds
     */
    void batchInsert(Long productDemandId, List<Long> customDemandIds);

    /**
     *
     * @param productDemandIds productDemandIds
     * @param customDemandId customDemandId
     */
    void batchInsert(List<Long> productDemandIds,Long customDemandId);

}
