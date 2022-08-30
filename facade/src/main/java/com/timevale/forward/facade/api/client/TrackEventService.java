package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.request.TrackEventDeleteReq;
import com.timevale.forward.facade.api.request.TrackEventModifyReq;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface TrackEventService {


    /**
     * 埋点地图
     *
     * @return 埋点事件
     */
    BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventQueryList trackEventQueryList);

    /**
     * 新增
     *
     * @param trackEventAddReq 埋点事件
     * @return Boolean
     */
    BaseResult<Boolean> add(TrackEventAddReq trackEventAddReq);

    /**
     * 新增
     *
     * @param trackEventModifyReq 埋点事件
     * @return Boolean
     */
    BaseResult<Boolean> modify(TrackEventModifyReq trackEventModifyReq);


    /**
     * 查看
     *
     * @param eventId 埋点事件
     * @return 详情信息
     */
    BaseResult<TrackEventDetailVO> get(Long eventId);


    /**
     * 删除
     *
     * @param trackEventDeleteReq 埋点事件
     * @return Boolean
     */
    BaseResult<Boolean> delete(TrackEventDeleteReq trackEventDeleteReq);
}
