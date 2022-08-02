package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.facade.api.client.BizLabelService;
import com.timevale.forward.facade.api.request.BizLabelAddReq;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class BizLabelServiceImpl implements BizLabelService {

    @Resource
    private TrackEventMapper trackEventMapper;

    @Override
    public BaseResult<Boolean> markOrUnMark(BizLabelAddReq bizLabelAddReq) {
        return BaseResult.success(true);
    }
}
