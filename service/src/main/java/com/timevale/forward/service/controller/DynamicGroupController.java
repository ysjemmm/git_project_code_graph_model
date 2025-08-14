package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.DynamicGroupService;
import com.timevale.forward.facade.api.query.DynamicProductDemandGroupList;
import com.timevale.forward.facade.api.result.DemandGroupNodeVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.service.utils.ResultUtils;
import com.timevale.mandarin.common.result.BusinessResult;
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

    @ApiOperation("叶子分组需求")
    @PostMapping("/productDemandList")
    public BusinessResult<QueryResultVO<ProductDemandVO>> getProductDemandList(@RequestBody DynamicProductDemandGroupList dynamicGroupQueryList) {
        return ResultUtils.result(dynamicGroupService.getProductDemandList(dynamicGroupQueryList));
    }
}
