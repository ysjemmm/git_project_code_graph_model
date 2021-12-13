package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.ProductLineService;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/13 17:07
 */
@Slf4j
@RestService
public class ProductLineServiceImpl implements ProductLineService {

    @Override
    public BaseResult<List<ProductLineVO>> list() {
        List<ProductLineVO> result = Lists.newArrayList();
        return BaseResult.success(result);
    }
}
