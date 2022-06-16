package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.TrackMapAddReq;
import com.timevale.forward.facade.api.request.TrackMapDeleteReq;
import com.timevale.forward.facade.api.result.TrackMapVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface TrackMapService {


    /**
     * 埋点地图
     *
     * @return 埋点地图
     */
    BaseResult<List<TrackMapVO>> trackMapList();

    /**
     * 新增
     *
     * @param trackMapAddReq 埋点地图
     * @return Boolean
     */
    BaseResult<Long> add(TrackMapAddReq trackMapAddReq);


    /**
     * 删除
     *
     * @param trackMapDeleteReq 埋点地图
     * @return Boolean
     */
    BaseResult<Boolean> delete(TrackMapDeleteReq trackMapDeleteReq);
}
