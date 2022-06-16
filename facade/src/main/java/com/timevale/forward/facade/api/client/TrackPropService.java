package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.TrackPropQueryList;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface TrackPropService {


    /**
     * 埋点地图
     *
     * @return 埋点事件
     */
    BaseResult<PageQueryResult<TrackPropVO>> list(TrackPropQueryList trackPropQueryList);


}
