package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.facade.api.client.BizChangeLogService;
import com.timevale.forward.facade.api.query.BizChangeLogQueryList;
import com.timevale.forward.facade.api.result.BizChangeLogVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizChangeLogCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@LogPoint
@RestService
public class BizChangeLogServiceImpl implements BizChangeLogService {

    @Resource
    BizChangeLogMapper bizChangeLogMapper;

    @Override
    public BaseResult<PageQueryResult<BizChangeLogVO>> list(BizChangeLogQueryList bizChangeLogQueryList) {
        // 开始分页
        PageHelper.startPage(bizChangeLogQueryList.pageNum, bizChangeLogQueryList.pageSize, CommonConstant.CREATE_ORDER_BY);

        // 查询转换
        List<BizChangeLogDO> bizChangeLogDOList = bizChangeLogMapper.list(bizChangeLogQueryList.getMainId(), bizChangeLogQueryList.getType());
        List<BizChangeLogVO> bizChangeLogVOList = BizChangeLogCopier.INSTANCE.convert(bizChangeLogDOList);

        // 返回分页数据
        PageInfo<BizChangeLogDO> pageInfo = new PageInfo<>(bizChangeLogDOList);
        PageQueryResult<BizChangeLogVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizChangeLogVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }
}
