package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.facade.api.client.TrackEventService;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.request.TrackEventDeleteReq;
import com.timevale.forward.facade.api.request.TrackEventModifyReq;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

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
    private TrackEventComponent trackEventComponent;


    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventQueryList trackEventQueryList) {
        if(trackEventQueryList.getTrackMapId()==null){
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        TrackEventListCondition condition = TrackEventCopier.INSTANCE.convert(trackEventQueryList);
        PageHelper.startPage(trackEventQueryList.getPageNum(), trackEventQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return trackEventComponent.list(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(TrackEventAddReq trackEventAddReq) {
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(TrackEventModifyReq trackEventModifyReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackEventDetailVO> get(Long eventId) {
        TrackEventDetailVO trackEventDetailVO = new TrackEventDetailVO();
        return BaseResult.success(trackEventDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(TrackEventDeleteReq trackEventDeleteReq) {
        return BaseResult.success(true);
    }


}
