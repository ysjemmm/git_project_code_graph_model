package com.timevale.forward.service.impl;

import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.LabelMapper;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.dal.entity.LabelDO;
import com.timevale.forward.facade.api.client.LabelService;
import com.timevale.forward.facade.api.query.LabelQueryList;
import com.timevale.forward.facade.api.request.LabelAddReq;
import com.timevale.forward.facade.api.request.LabelModifyReq;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
import com.timevale.forward.facade.api.result.LabelDetailVO;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class LabelServiceImpl implements LabelService {

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private BizLabelMapper bizLabelMapper;


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
    public BaseResult<Boolean> delete(Long labelId) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> add(LabelAddReq labelAddReq) {
        List<String> names = labelAddReq.getNames();
        Long categoryId = labelAddReq.getCategoryId();
        List<LabelDO> labelDOList = labelMapper.getByNameInOneCategory(names, categoryId);
        if(!CollectionUtils.isEmpty(labelDOList)){
            throw new BaseBizRuntimeException("名称为: "+names+" 的标签,已在该标签类别下存在,请修改后重试");
        }
        List<LabelDO> labelDos = names.stream().map(a -> {
            LabelDO o = new LabelDO();
            o.setLabelCategoryId(categoryId);
            o.setName(a);
            return o;
        }).collect(Collectors.toList());
        labelMapper.batchInsert(labelDos);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(LabelModifyReq labelModifyReq) {
        LabelDO labelDO=new LabelDO();
        labelDO.setId(labelModifyReq.getId());
        labelDO.setName(labelModifyReq.getName());
        labelMapper.update(labelDO);
        return BaseResult.success(true);
    }

}
