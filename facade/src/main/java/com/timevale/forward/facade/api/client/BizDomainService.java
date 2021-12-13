package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/13 17:02
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface BizDomainService {

    /**
     * 列表
     *
     * @return 返回业务域列表
     */
    BaseResult<List<BizDomainVO>> list();
}
