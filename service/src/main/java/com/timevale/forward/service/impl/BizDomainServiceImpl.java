package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.result.BizDomainVO;
import org.assertj.core.util.Lists;

import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/13 17:10
 */
public class BizDomainServiceImpl implements BizDomainService {

    @Override
    public BaseResult<List<BizDomainVO>> list() {
        List<BizDomainVO> result = Lists.newArrayList();
        return BaseResult.success(result);
    }
}
