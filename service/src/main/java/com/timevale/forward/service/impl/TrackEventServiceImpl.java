package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventCondition;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.client.TrackEventService;
import com.timevale.forward.facade.api.query.TrackEventQueryList;
import com.timevale.forward.facade.api.request.TrackEventAddReq;
import com.timevale.forward.facade.api.request.TrackEventDeleteReq;
import com.timevale.forward.facade.api.request.TrackEventModifyReq;
import com.timevale.forward.facade.api.result.TrackEventDetailVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.model.enums.TrackStatusEnum;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class TrackEventServiceImpl implements TrackEventService {

    @Resource
    private TrackEventMapper trackEventMapper;

    @Resource
    private TrackEventComponent trackEventComponent;


    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventQueryList trackEventQueryList) {
        if (trackEventQueryList.getTrackMapId() == null) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        TrackEventListCondition condition = TrackEventCopier.INSTANCE.convert(trackEventQueryList);
        PageHelper.startPage(trackEventQueryList.getPageNum(), trackEventQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return trackEventComponent.list(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(TrackEventAddReq trackEventAddReq) {
        checkBeforeInsert(trackEventAddReq);
        TrackEventDO trackEventDO = TrackEventCopier.INSTANCE.convert(trackEventAddReq);
        Long trackMapId = trackEventAddReq.getElementId() == null ? trackEventAddReq.getPageId() : trackEventAddReq.getElementId();
        trackEventDO.setTrackMapId(trackMapId);
        trackEventDO.setFlowId("");
        trackEventDO.setStatus(TrackStatusEnum.REVIEWING.getCode());
        trackEventMapper.insert(trackEventDO);
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

    private void checkBeforeInsert(TrackEventAddReq trackEventAddReq) {
        TrackEventCondition c = TrackEventCondition.builder().cnName(trackEventAddReq.getCnName()).build();
        List<TrackEventDO> trackEventDos = trackEventMapper.select(c);
        boolean match = trackEventDos.stream().anyMatch(a -> a.getCnName().equals(trackEventAddReq.getCnName()));
        if (match) {
            throw new BaseBizRuntimeException("该事件中文名重复,请修改后重试");
        }

        c = TrackEventCondition.builder().egName(trackEventAddReq.getEgName()).build();
        trackEventDos = trackEventMapper.select(c);
        match = trackEventDos.stream().anyMatch(a -> a.getEgName().equals(trackEventAddReq.getEgName()));
        if (match) {
            throw new BaseBizRuntimeException("该事件英文名重复,请修改后重试");
        }
    }
}
