package com.timevale.forward.service.component;

import com.timevale.forward.facade.api.request.ProjectProductDemandLinkReq;

import java.util.List;

public interface ProjectProductDemandComponent {

    /**
     * 关联or取消关联
     *
     * @param productDemandLinkReq 产品需求
     */
    void linkOrUnLinkProductDemand(ProjectProductDemandLinkReq productDemandLinkReq);

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
