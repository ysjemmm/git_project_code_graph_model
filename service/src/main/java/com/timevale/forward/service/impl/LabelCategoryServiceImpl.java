package com.timevale.forward.service.impl;

import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.TrackEvenPropMapper;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.dao.TrackPropMapper;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.facade.api.client.LabelCategoryService;
import com.timevale.forward.facade.api.query.LabelCategoryQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.result.LabelCategoryDetailVO;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
import com.timevale.forward.facade.api.result.LabelSimpleVO;
import com.timevale.forward.service.component.TrackPropComponent;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.ArrayList;
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
    public BaseResult<PageQueryResult<LabelCategoryVO>> list(LabelCategoryQueryList labelCategoryQueryList) {
        List<LabelCategoryVO>labelCategoryVOList=new ArrayList<>();
        List<LabelCategoryDO>labelCategoryDOList=new ArrayList<>();

        PageInfo<LabelCategoryDO> pageInfo = new PageInfo<>(labelCategoryDOList);
        PageQueryResult<LabelCategoryVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(labelCategoryVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<LabelSimpleVO>> getAll() {
        List<LabelSimpleVO> labelSimpleVOList = Lists.newArrayList(new LabelSimpleVO());
        return BaseResult.success(labelSimpleVOList);
    }

    @Override
    public BaseResult<List<LabelCategorySimpleVO>> getLabelInCategory(LabelInCategoryQueryList labelInCategoryQueryList) {
        List<LabelCategorySimpleVO> labelSimpleVOList = Lists.newArrayList(new LabelCategorySimpleVO());
        return BaseResult.success(labelSimpleVOList);
    }

    @Override
    public BaseResult<LabelCategoryDetailVO> get(Long categoryId) {
        return BaseResult.success(new LabelCategoryDetailVO());
    }

    @Override
    public BaseResult<Boolean> delete(Long categoryId) {
        return BaseResult.success(true);
    }


    @Override
    public BaseResult<Boolean> add(LabelCategoryAddReq labelCategoryAddReq) {
        return BaseResult.success(true);
    }


}
