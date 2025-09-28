package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.ProductDemandGroupService;
import com.timevale.forward.facade.api.query.ProductDemandGroupQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.ProductDemandGroupVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.service.utils.ResultUtils;
import com.timevale.mandarin.common.result.BusinessResult;
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

    @ApiOperation("产品需求组内新增")
    @PostMapping("/addDemand")
    public BusinessResult<Boolean> addDemand(@RequestBody @Valid ProductDemandGroupInnerAddReq productDemandGroupInnerAddReq) {
        return ResultUtils.result(productDemandGroupService.addDemand(productDemandGroupInnerAddReq));
    }

    @ApiOperation("查询待排序的产品需求")
    @PostMapping("/listProductDemandBacklog")
    public BusinessResult<PageQueryResult<ProductDemandVO>> listProductDemandBacklog(@RequestBody @Valid ProductDemandGroupQueryList productDemandGroupQueryList) {
        return ResultUtils.result(productDemandGroupService.listProductDemandBacklog(productDemandGroupQueryList));
    }

    @ApiOperation("产品需求分组列表")
    @PostMapping("/list")
    public BusinessResult<PageQueryResult<ProductDemandGroupVO>> listProductDemandGroup(@RequestBody @Valid ProductDemandGroupQueryList productDemandGroupQueryList) {
        return ResultUtils.result(productDemandGroupService.listProductDemandGroup(productDemandGroupQueryList));
    }

    @ApiOperation("产品需求分组")
    @GetMapping("/get")
    public BusinessResult<ProductDemandGroupVO> get(@RequestParam Long id) {
        return ResultUtils.result(productDemandGroupService.getProductDemandGroupById( id));
    }

    @ApiOperation("产品需求分组新增")
    @PostMapping("/add")
    public BusinessResult<Boolean> add(@RequestBody @Valid ProductDemandGroupAddReq productDemandGroupAddReq) {
        return ResultUtils.result(productDemandGroupService.add(productDemandGroupAddReq));
    }

    @ApiOperation("产品需求分组修改")
    @PostMapping("/modify")
    public BusinessResult<Boolean> modify(@RequestBody @Valid ProductDemandGroupModifyReq productDemandGroupModifyReq) {
        return ResultUtils.result(productDemandGroupService.modify(productDemandGroupModifyReq));
    }

    @ApiOperation("产品需求分组删除")
    @PostMapping("/delete")
    public BusinessResult<Boolean> delete(@RequestBody @Valid ProductDemandGroupReq productDemandGroupReq) {
        return ResultUtils.result(productDemandGroupService.delete(productDemandGroupReq));
    }

    @ApiOperation("产品需求拖动")
    @PostMapping("/moveProductDemand")
    public BusinessResult<Boolean> moveProductDemand(@RequestBody @Valid ProductDemandGroupItemMoveReq productDemandGroupItemMoveReq) {
        return ResultUtils.result(productDemandGroupService.moveProductDemand(productDemandGroupItemMoveReq));
    }

    @ApiOperation("产品需求分组拖动")
    @PostMapping("/moveProductDemandGroup")
    public BusinessResult<Boolean> moveProductDemandGroup(@RequestBody @Valid ProductDemandGroupMoveReq productDemandGroupMoveReq) {
        return ResultUtils.result(productDemandGroupService.moveProductDemandGroup(productDemandGroupMoveReq));
    }

    @ApiOperation("关联或取消关联项目")
    @PostMapping("/linkOrUnlinkProject")
    public BusinessResult<Boolean> linkOrUnlinkProject(@RequestBody @Valid ProductDemandGroupProjectLinkReq productDemandGroupItemMoveReq) {
        return ResultUtils.result(productDemandGroupService.linkOrUnlinkProject(productDemandGroupItemMoveReq));
    }

}
