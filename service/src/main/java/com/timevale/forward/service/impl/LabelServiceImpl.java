package com.timevale.forward.service.impl;

import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.facade.api.client.LabelService;
import com.timevale.forward.facade.api.query.LabelQueryList;
import com.timevale.forward.facade.api.request.LabelAddReq;
import com.timevale.forward.facade.api.request.LabelModifyReq;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
import com.timevale.forward.facade.api.result.LabelDetailVO;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class LabelServiceImpl implements LabelService {

    @Resource
    private TrackEventMapper trackEventMapper;


    @Override
    public BaseResult<PageQueryResult<LabelCategoryVO>> list(LabelQueryList labelQueryList) {
        List<LabelCategoryVO>labelCategoryVOList=new ArrayList<>();
        List<LabelCategoryDO>labelCategoryDOList=new ArrayList<>();

        PageInfo<LabelCategoryDO> pageInfo = new PageInfo<>(labelCategoryDOList);
        PageQueryResult<LabelCategoryVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(labelCategoryVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<LabelDetailVO> get(Long labelId) {
        return BaseResult.success(new LabelDetailVO());
    }

    @Override
    public BaseResult<Boolean> delete(Long categoryId) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> add(LabelAddReq labelAddReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(LabelModifyReq labelModifyReq) {
        return BaseResult.success(true);
    }

}
