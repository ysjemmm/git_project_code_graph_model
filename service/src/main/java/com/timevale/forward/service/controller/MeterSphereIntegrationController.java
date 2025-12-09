package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.*;
import com.timevale.forward.facade.api.result.*;
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
 * @author mayang
 * @date 2025/11/28
 **/
@RestController
@Api(tags = "供metersphere调用的产研接口")
@Slf4j
@RequestMapping("/forward/metersphere/integration")
public class MeterSphereIntegrationController {

    @Resource
    private ProductDemandService productDemandService;

    @Resource
    private BizDomainService bizDomainService;

    @ApiOperation("产品需求列表")
    @PostMapping("/productDemand/list")
    public BusinessResult<QueryResultVO<ProductDemandVO>> listProductDemands(@RequestBody @Valid ProductDemandQueryList productDemandQueryList) {
        return ResultUtils.result(productDemandService.simpleList(productDemandQueryList));
    }

    @ApiOperation("业务域列表")
    @PostMapping("/bizDomain/list")
    public BusinessResult<PageQueryResult<BizDomainVO>> listBizDomains(@RequestBody @Valid BizDomainQueryList bizDomainQueryList) {
        return ResultUtils.result(bizDomainService.simpleList(bizDomainQueryList));
    }

}
