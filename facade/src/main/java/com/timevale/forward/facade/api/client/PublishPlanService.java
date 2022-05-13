package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectLinkPublishPlanQueryList;
import com.timevale.forward.facade.api.query.PublishPlanQueryList;
import com.timevale.forward.facade.api.request.ProjectPublishPlanLinkReq;
import com.timevale.forward.facade.api.result.PublishPlanVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface PublishPlanService {

    /**
     *
     *
     * @param publishPlanQueryList publishPlanQueryList
     * @return 列表
     */
    BaseResult<PageQueryResult<PublishPlanVO>> matchPublishPlan(PublishPlanQueryList publishPlanQueryList);

    /**
     *
     * @param projectPublishPlanLinkReq projectPublishPlanLinkReq
     * @return ProductDemandStatusVO
     */
    BaseResult<Boolean> linkOrUnLinkPublishPlan(ProjectPublishPlanLinkReq projectPublishPlanLinkReq);

    /**
     *
     * @param projectLinkPublishPlanQueryList projectLinkPublishPlanQueryList
     * @return PublishPlanVO
     */
    BaseResult<PageQueryResult<PublishPlanVO>> linkPublishPlanList(ProjectLinkPublishPlanQueryList projectLinkPublishPlanQueryList);

}
