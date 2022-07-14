package com.timevale.forward.service.component;

import java.util.List;

public interface ProductCustomDemandComponent {


    /**
     *
     * @param productDemandId productDemandId
     * @param bizDemandId bizDemandId
     */
    void update(Long productDemandId,Long bizDemandId);

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
