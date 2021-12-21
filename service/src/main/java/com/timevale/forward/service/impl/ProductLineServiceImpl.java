package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.facade.api.client.ProductLineService;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/13 17:07
 */
@Slf4j
@RestService
public class ProductLineServiceImpl implements ProductLineService {

    @Resource
    ProductLineMapper productLineMapper;

    @Override
    public BaseResult<List<ProductLineVO>> productLineList() {
        List<ProductLineVO> result = ProductLineCopier.INSTANCE.convert(productLineMapper.selectAllProductLine());
        return BaseResult.success(result);
    }
}
