package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.entity.TrackPropDO;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
public interface TrackPropComponent {

    /**
     * 埋点地图
     *
     * @return 埋点事件
     */
    BaseResult<PageQueryResult<TrackPropVO>> list(TrackPropListCondition condition);

    /**
     * 埋点地图
     *
     * @return 埋点事件
     */
    BaseResult<Boolean> add(List<TrackPropDO>trackPropDOList,Long trackEventId);
}
