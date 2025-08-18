package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.DynamicGroupService;
import com.timevale.forward.facade.api.query.DynamicBizDemandGroupList;
import com.timevale.forward.facade.api.query.DynamicProductDemandGroupList;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.DemandGroupNodeVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.service.utils.ResultUtils;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

;

/**
 * @auther: yuhua
 * @date: 2025/8/13 15:00
 * @description: 需求分组
 */
@RestController
@Api(tags = "动态分组")
@Slf4j
@RequestMapping("/forward/group")
public class DynamicGroupController {

    @Resource
    private DynamicGroupService dynamicGroupService;

    @ApiOperation("产品需求分组树")
    @PostMapping("/productDemandsGroupTree")
    public BusinessResult<List<DemandGroupNodeVO>> getProductDemandsGroupTree(@RequestBody DynamicProductDemandGroupList dynamicGroupQueryList) {
        return ResultUtils.result(dynamicGroupService.getProductDemandsGroupTree(dynamicGroupQueryList));
    }

    @ApiOperation("产品需求列表")
    @PostMapping("/productDemandList")
    public BusinessResult<PageQueryResult<ProductDemandVO>> getProductDemandList(@RequestBody DynamicProductDemandGroupList dynamicGroupQueryList) {
        return ResultUtils.result(dynamicGroupService.getProductDemandList(dynamicGroupQueryList));
    }

    @ApiOperation("业务需求分组树")
    @PostMapping("/bizDemandsGroupTree")
    public BusinessResult<List<DemandGroupNodeVO>> getBizDemandsGroupTree(@RequestBody DynamicBizDemandGroupList dynamicGroupQueryList) {
        return ResultUtils.result(dynamicGroupService.getBizDemandsGroupTree(dynamicGroupQueryList));
    }

    @ApiOperation("业务需求列表")
    @PostMapping("/bizDemandList")
    public BusinessResult<PageQueryResult<BizDemandVO>> getBizDemandList(@RequestBody DynamicBizDemandGroupList dynamicGroupQueryList) {
        return ResultUtils.result(dynamicGroupService.getBizDemandList(dynamicGroupQueryList));
    }
}
