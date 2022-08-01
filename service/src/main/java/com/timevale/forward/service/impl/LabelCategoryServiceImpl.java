package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.TrackEvenPropMapper;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.dao.TrackPropMapper;
import com.timevale.forward.facade.api.client.LabelCategoryService;
import com.timevale.forward.facade.api.query.LabelCategoryQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.service.component.TrackPropComponent;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class LabelCategoryServiceImpl implements LabelCategoryService {

    @Resource
    private TrackPropComponent trackPropComponent;

    @Resource
    private TrackPropMapper trackPropMapper;

    @Resource
    private TrackEvenPropMapper trackEvenPropMapper;

    @Resource
    private TrackEventMapper trackEventMapper;


    @Override
    public BaseResult<PageQueryResult<TrackPropVO>> list(LabelCategoryQueryList labelCategoryQueryList) {
        return null;
    }

    @Override
    public BaseResult<List<LabelCategorySimpleVO>> getLabelInCategory(LabelInCategoryQueryList labelInCategoryQueryList) {
        return null;
    }


    @Override
    public BaseResult<Boolean> add(LabelCategoryAddReq labelCategoryAddReq) {
        return BaseResult.success(true);
    }


}
