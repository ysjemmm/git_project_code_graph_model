package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.query.BizDomainQueryList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.service.utils.ResultUtils;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private BizDemandService bizDemandService;

    @Resource
    private BizDomainService bizDomainService;

    @Resource
    private ProjectService projectService;

    @ApiOperation("产品需求列表")
    @PostMapping("/productDemand/list")
    public BusinessResult<QueryResultVO<ProductDemandVO>> listProductDemands(@RequestBody @Valid ProductDemandQueryList productDemandQueryList) {
        return ResultUtils.result(productDemandService.simpleList(productDemandQueryList));
    }

    @ApiOperation("业务需求列表")
    @PostMapping("/bizDemand/list")
    public BusinessResult<QueryResultVO<BizDemandVO>> listBizDemands(@RequestBody @Valid BizDemandQueryList bizDemandQueryList) {
        return ResultUtils.result(bizDemandService.simpleList(bizDemandQueryList));
    }

    @ApiOperation("业务域列表")
    @PostMapping("/bizDomain/list")
    public BusinessResult<PageQueryResult<BizDomainVO>> listBizDomains(@RequestBody @Valid BizDomainQueryList bizDomainQueryList) {
        return ResultUtils.result(bizDomainService.simpleList(bizDomainQueryList));
    }

    @ApiOperation("填充用例完成/发布完成时间")
    @PostMapping("/project/autoCompleteTime")
    public BusinessResult<Boolean> autoCompleteTime(@RequestBody ProjectNodeAddReq projectNodeAddReq) {
        return ResultUtils.result(projectService.autoCompleteTime(projectNodeAddReq));
    }

}
