package com.timevale.forward.service.controller;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.ProductDemandGroupService;
import com.timevale.forward.facade.api.query.ProductDemandGroupQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandGroupVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.mandarin.common.result.PageQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

/**
 * @title: ProductDemandGroupController
 * @Author qiyuan
 * @Date 2025/7/15 20:22
 */
@RestController
@Api(tags = "产品需求分组管理")
@Slf4j
@RequestMapping("/forward/productDemand/group")
public class ProductDemandGroupController {

    @Resource
    private ProductDemandGroupService productDemandGroupService;

    @ApiOperation("查询待排序的产品需求")
    @PostMapping("/listProductDemandBacklog")
    public BaseResult<PageQueryResult<ProductDemandVO>> listProductDemandBacklog(@RequestBody @Valid ProductDemandGroupQueryList productDemandGroupQueryList) {
        return productDemandGroupService.listProductDemandBacklog(productDemandGroupQueryList);
    }

    @ApiOperation("产品需求分组列表")
    @PostMapping("/list")
    public BaseResult<PageQueryResult<ProductDemandGroupVO>> listProductDemandGroup(@RequestBody @Valid ProductDemandGroupQueryList productDemandGroupQueryList) {
        return productDemandGroupService.listProductDemandGroup(productDemandGroupQueryList);
    }

    @ApiOperation("产品需求分组")
    @GetMapping("/get")
    public BaseResult<ProductDemandGroupVO> get(@RequestParam Long id) {
        return productDemandGroupService.getProductDemandGroupById( id);
    }

    @ApiOperation("产品需求分组新增")
    @PostMapping("/add")
    public BaseResult<Boolean> add(@RequestBody @Valid ProductDemandGroupAddReq productDemandGroupAddReq) {
        return productDemandGroupService.add(productDemandGroupAddReq);
    }

    @ApiOperation("产品需求分组修改")
    @PostMapping("/modify")
    public BaseResult<Boolean> modify(@RequestBody @Valid ProductDemandGroupModifyReq productDemandGroupModifyReq) {
        return productDemandGroupService.modify(productDemandGroupModifyReq);
    }

    @ApiOperation("产品需求分组删除")
    @PostMapping("/delete")
    public BaseResult<Boolean> delete(@RequestBody @Valid ProductDemandGroupReq productDemandGroupReq) {
        return productDemandGroupService.delete(productDemandGroupReq);
    }

    @ApiOperation("产品需求分组绑定项目")
    @PostMapping("/bindProject")
    public BaseResult<Boolean> bindProject(@RequestBody @Valid ProductDemandGroupProjectReq productDemandGroupProjectReq) {
        return productDemandGroupService.bindProject(productDemandGroupProjectReq);
    }

    @ApiOperation("产品需求拖动")
    @PostMapping("/moveProductDemand")
    public BaseResult<Boolean> moveProductDemand(@RequestBody @Valid ProductDemandGroupMoveReq productDemandGroupMoveReq) {
        return productDemandGroupService.moveProductDemand(productDemandGroupMoveReq);
    }

    @ApiOperation("产品需求分组拖动")
    @PostMapping("/moveProductDemandGroup")
    public BaseResult<Boolean> moveProductDemandGroup(@RequestBody @Valid ProductDemandGroupMoveReq productDemandGroupMoveReq) {
        return productDemandGroupService.moveProductDemandGroup(productDemandGroupMoveReq);
    }

}
