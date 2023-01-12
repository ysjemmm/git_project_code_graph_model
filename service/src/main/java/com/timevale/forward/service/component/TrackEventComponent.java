package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
public interface TrackEventComponent {

    /**
     * 埋点查询-全部
     *
     * @param condition 条件
     * @return {@link List}<{@link TrackEventVO}>
     */
    List<TrackEventVO> listAll(TrackEventListCondition condition);

    /**
     * 埋点查询-分页
     *
     * @return 埋点事件
     */
    BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventListCondition condition);


    /**
     * 埋点事件
     * @param processInstanceId processInstanceId
     */
    void  updateTrackEventInfo(String processInstanceId);

    /**
     * 更新 埋点事件-属性
     * @param trackEventDO trackEventDO
     */
    void updateTrackEventProp(TrackEventDO trackEventDO);


    String startFlow(TrackEventAddReq trackEventAddReq, UserInfo userInfo);

    void updateFlowId(List<TrackEventDO> trackEventDOList, UserInfo userInfo);
}
