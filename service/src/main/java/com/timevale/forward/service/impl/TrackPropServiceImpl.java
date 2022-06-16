package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.facade.api.client.TrackPropService;
import com.timevale.forward.facade.api.query.TrackPropQueryList;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.service.component.TrackPropComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackPropCopier;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class TrackPropServiceImpl implements TrackPropService {


    @Resource
    private TrackPropComponent trackPropComponent;


    @Override
    public BaseResult<PageQueryResult<TrackPropVO>> list(TrackPropQueryList trackPropQueryList) {
        TrackPropListCondition condition = TrackPropCopier.INSTANCE.convert(trackPropQueryList);
        PageHelper.startPage(trackPropQueryList.getPageNum(), trackPropQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return trackPropComponent.list(condition);
    }

}
