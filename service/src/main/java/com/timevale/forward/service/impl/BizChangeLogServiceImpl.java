package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.BizChangeLogService;
import com.timevale.forward.facade.api.query.BizChangeLogQueryList;
import com.timevale.forward.facade.api.result.BizChangeLogVO;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@LogPoint
@RestService
public class BizChangeLogServiceImpl implements BizChangeLogService {

    @Override
    public BaseResult<PageQueryResult<BizChangeLogVO>> list(BizChangeLogQueryList bizChangeLogQueryList) {
        return BaseResult.success(ResultUtil.pageEmpty());
    }
}
