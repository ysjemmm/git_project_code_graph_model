package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ModelCondition;
import com.timevale.forward.dal.dao.ModelMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.ModelService;
import com.timevale.forward.facade.api.query.ModelQueryList;
import com.timevale.forward.facade.api.request.ModelAddReq;
import com.timevale.forward.facade.api.request.ModelModifyReq;
import com.timevale.forward.facade.api.result.ModelVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ModelCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/13 17:07
 */
@Slf4j
@RestService
public class ModelServiceImpl implements ModelService {

    @Resource
    ModelMapper modelMapper;

    @Resource
    ProductLineMapper productLineMapper;

    @Override
    public BaseResult<PageQueryResult<ModelVO>> modelList(ModelQueryList modelQueryList) {
        ModelCondition condition = ModelCopier.INSTANCE.convert(modelQueryList);

        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();
        Map<Long, ProductLineDO> productLineMap = productLineDOList.stream()
                .collect(Collectors.toMap(ProductLineDO::getId, a -> a, (v1, v2) -> v2));

        PageHelper.startPage(modelQueryList.pageNum, modelQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        List<ModelDO> modelDOList = modelMapper.selectByCondition(condition);

        List<ModelVO> modelVOList = ModelCopier.INSTANCE.convert(modelDOList);
        modelVOList.forEach(e -> {
            ProductLineDO productLineDO = productLineMap.get(e.getProductLineId());
            e.setProductLineName(productLineDO.getName());
        });
        PageInfo<ModelDO> pageInfo = new PageInfo<>(modelDOList);
        PageQueryResult<ModelVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(modelVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> add(ModelAddReq modelAddReq) {
        ModelDO modelDO = ModelCopier.INSTANCE.convert(modelAddReq);
        modelMapper.insert(modelDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> update(ModelModifyReq modelModifyReq) {
        ModelDO modelDO = ModelCopier.INSTANCE.convert(modelModifyReq);
        modelMapper.update(modelDO);
        return BaseResult.success(true);
    }
}
