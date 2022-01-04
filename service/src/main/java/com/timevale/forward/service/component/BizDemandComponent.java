package com.timevale.forward.service.component;

import com.timevale.forward.facade.api.result.BizDemandStatusVO;

/**
 * @author by YangXu
 * @date 2022/01/04 10:26
 */
public interface BizDemandComponent {

    /**
     * 更新业务需求状态根据关联的产品需求
     *
     * @param bizDemandId 业务需求id
     */
    BizDemandStatusVO updateBizDemandStatusAsLinkProductDemand(Long bizDemandId);

}
