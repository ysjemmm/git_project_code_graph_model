package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProductDemandIdReq;
import com.timevale.forward.facade.api.result.ProductDemandDescRecordVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author jingchun
 * create on 2022/7/1
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProductDemandDescRecordService {

    BaseResult<List<ProductDemandDescRecordVO>> list(ProductDemandIdReq productDemandIdReq);

}
