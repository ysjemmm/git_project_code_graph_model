package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
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
public class ProjectServiceImpl implements ProjectService {
    @Override
    public BaseResult<PageQueryResult<ProjectVO>> list(ProjectQueryList projectQueryList) {
        log.info("项目列表接收参数:{}", projectQueryList);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProjectVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProjectVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Integer> updateStatus(Long id, Byte type) {
        log.info("项目暂停或作废接收参数:id={},type={}", id, type);
        return BaseResult.success(1);
    }

    @Override
    public BaseResult<Integer> enable(Long id) {
        log.info("项目开启接收参数:id={}", id);
        return BaseResult.success(1);
    }


    @Override
    public BaseResult<Integer> add(ProjectAddReq projectAddReq) {
        log.info("项目新增接收参数:{}", projectAddReq);
        return BaseResult.success(1);
    }

    @Override
    public BaseResult<Integer> modify(ProjectModifyReq projectModifyReq) {
        log.info("项目修改接收参数:{}", projectModifyReq);
        return BaseResult.success(1);
    }

    @Override
    public BaseResult<ProjectDetailVO> get(Long id) {
        log.info("项目查看接收参数:id={}", id);
        return BaseResult.success(new ProjectDetailVO());
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(Long id) {
        log.info("产品需求匹配接收参数:id={}", id);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProductDemandVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProductDemandVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Integer> linkOrUnLinkProductDemand(Long id,Long productDemandId, Byte type) {
        log.info("关联or取消关联接收参数:id={},productDemandId={},type={}", id,productDemandId, type);
        return BaseResult.success(1);
    }
}
