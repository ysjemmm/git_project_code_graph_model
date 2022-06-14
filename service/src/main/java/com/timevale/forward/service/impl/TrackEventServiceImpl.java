package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ModelMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.facade.api.client.TrackEventService;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
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
public class TrackEventServiceImpl implements TrackEventService {

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private ModelMapper modelMapper;


    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventQueryList trackEventQueryList) {
        return BaseResult.success();
    }

    @Override
    public BaseResult<Boolean> add(TrackEventAddReq trackEventAddReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(TrackEventModifyReq trackEventModifyReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackEventDetailVO> get(Long eventId) {
        TrackEventDetailVO trackEventDetailVO = new TrackEventDetailVO();
        return BaseResult.success(trackEventDetailVO);
    }

    @Override
    public BaseResult<Boolean> delete(TrackEventDeleteReq trackEventDeleteReq) {
        return BaseResult.success(true);
    }


}
