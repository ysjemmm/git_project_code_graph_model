package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.request.BizDemandLinkProjectReq;
import com.timevale.forward.facade.api.request.BizDemandUnlinkProjectReq;

public interface BizDemandProjectService {

    /**
     * 关联业务需求和项目
     */
    BaseResult<Void> linkBizDemandProject(BizDemandLinkProjectReq bizDemandLinkProjectReq);

    /**
     * 取消业务需求和项目的关联关系
     */
    BaseResult<Void> unlinkBizDemandProject(BizDemandUnlinkProjectReq bizDemandUnlinkProjectReq);

}
