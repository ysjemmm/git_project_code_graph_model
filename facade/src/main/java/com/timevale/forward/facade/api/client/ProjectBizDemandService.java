package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.BizDemandLinkProjectReq;
import com.timevale.mandarin.common.annotation.RestClient;

@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectBizDemandService {

    /**
     * 关联或者取消关联业务需求
     */
    BaseResult<Void> linkOrUnlinkBizDemandProject(BizDemandLinkProjectReq bizDemandLinkProjectReq);

}
