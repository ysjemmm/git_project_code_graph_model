package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.mandarin.common.result.PageQueryResult;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
public interface TrackEventComponent {

    /**
     * 埋点地图
     *
     * @return 埋点事件
     */
    BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventListCondition condition);
}
