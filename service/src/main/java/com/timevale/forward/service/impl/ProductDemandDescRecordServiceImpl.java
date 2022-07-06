package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandDescRecordMapper;
import com.timevale.forward.dal.entity.ProductDemandDescRecordDO;
import com.timevale.forward.facade.api.client.ProductDemandDescRecordService;
import com.timevale.forward.facade.api.request.ProductDemandIdReq;
import com.timevale.forward.facade.api.result.ProductDemandDescRecordVO;
import com.timevale.forward.service.copy.ProductDemandDescRecordCopier;
import com.timevale.mandarin.common.annotation.RestService;

import javax.annotation.Resource;
import java.util.List;

/**
 * 产品需求历史记录相关接口
 *
 * @author jingchun
 * create on 2022/7/4
 */
@RestService
public class ProductDemandDescRecordServiceImpl implements ProductDemandDescRecordService {

    @Resource
    private ProductDemandDescRecordMapper productDemandDescRecordMapper;

    @Override
    public BaseResult<List<ProductDemandDescRecordVO>> list(
            ProductDemandIdReq productDemandIdReq) {

        Long productDemandId = productDemandIdReq.getProductDemandId();
        List<ProductDemandDescRecordDO> descriptions =
                productDemandDescRecordMapper.listByProductDemandId(productDemandId);

        return BaseResult.success(ProductDemandDescRecordCopier.INSTANCE.convert(descriptions));
    }
}
