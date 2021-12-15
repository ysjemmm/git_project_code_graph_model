package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.service.copy.BizDomainCopier;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/13 17:10
 */
@Slf4j
@RestService
public class BizDomainServiceImpl implements BizDomainService {

    // @Resource
    BizDomainMapper bizDomainMapper;

    @Override
    public BaseResult<List<BizDomainVO>> bizDomainList() {
        List<BizDomainVO> result = BizDomainCopier.INSTANCE.convert(bizDomainMapper.selectAllBizDomain());
        return BaseResult.success(result);
    }
}
