package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizRecordMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizRecordDO;
import com.timevale.forward.facade.api.client.BizChangeLogService;
import com.timevale.forward.facade.api.query.BizChangeLogQueryList;
import com.timevale.forward.facade.api.result.BizChangeLogVO;
import com.timevale.forward.facade.api.result.BizRecordVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizChangeLogCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@LogPoint
@RequiredArgsConstructor
public class BizChangeLogServiceImpl implements BizChangeLogService {
    private final BizRecordMapper bizRecordMapper;
    private final BizChangeLogMapper bizChangeLogMapper;

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

    @Override
    public BaseResult<PageQueryResult<BizRecordVO>> statusOperator(BizChangeLogQueryList query) {
        // 分页查询
        PageHelper.startPage(query.pageNum, query.pageSize, CommonConstant.CREATE_ORDER_BY);
        List<BizRecordDO> recordDOList = bizRecordMapper.get(query.getMainId(), query.getType());

        // 转换
        List<BizRecordVO> recordVOList = recordDOList.stream()
                .map(e -> JSON.parseObject(e.getRecord(), BizRecordVO.class))
                .collect(Collectors.toList());

        // 返回分页数据
        PageInfo<BizRecordDO> pageInfo = new PageInfo<>(recordDOList);
        PageQueryResult<BizRecordVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(recordVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }
}
