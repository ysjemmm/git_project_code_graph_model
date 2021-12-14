package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:44
 **/
@Slf4j
@RestService
public class ProductDemandServiceImpl implements ProductDemandService {
    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList) {
        log.info("产品需求接收参数:{}", productDemandQueryList);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProductDemandVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProductDemandVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Integer> updateStatus(Long id, Byte type) {
        log.info("产品需求暂停或开启收参数:id={},type={}", id, type);
        return BaseResult.success(1);
    }

    @Override
    public BaseResult<Integer> add(ProductDemandAddReq productDemandAddReq) {
        log.info("产品需求新增接收参数:{}", productDemandAddReq);
        return BaseResult.success(1);
    }

    @Override
    public BaseResult<Integer> modify(ProductDemandModifyReq productDemandModifyReq) {
        log.info("产品需求修改接收参数:{}", productDemandModifyReq);
        return BaseResult.success(1);
    }

    @Override
    public BaseResult<ProductDemandDetailVO> get(Long id) {
        log.info("产品需求查看接收参数:id={}", id);
        return BaseResult.success(new ProductDemandDetailVO());
    }

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> matchProjectList(Long id) {
        log.info("产品需求匹配接收参数:id={}", id);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProjectVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProjectVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> matchBizDemandList(Long id) {
        log.info("业务需求匹配接收参数:id={}", id);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<BizDemandVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new BizDemandVO()));
        return BaseResult.success(result);
    }

}
