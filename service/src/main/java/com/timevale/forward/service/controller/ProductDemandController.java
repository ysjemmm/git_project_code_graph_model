package com.timevale.forward.service.controller;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.PersonService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.ProductBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductCustomDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkProjectQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkTrackEventQueryList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandTrackEventQueryList;
import com.timevale.forward.facade.api.query.ProductLinkCustomDemandQueryList;
import com.timevale.forward.facade.api.request.BatchTransferReq;
import com.timevale.forward.facade.api.request.ProductBizDemandLinkReq;
import com.timevale.forward.facade.api.request.ProductCustomDemandLinkReq;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.request.ProductDemandTrackEventLinkReq;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.service.utils.ResultUtils;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestController
@Api(tags = "产品需求管理")
@Slf4j
@RequestMapping("/forward/productDemand")
public class ProductDemandController {

    @Resource
    private ProductDemandService productDemandService;

    @Resource
    private PersonService personService;

    @ApiOperation("产品需求列表")
    @PostMapping("/list")
    public BusinessResult<QueryResultVO<ProductDemandVO>> list(@RequestBody @Valid ProductDemandQueryList productDemandQueryList) {
        return ResultUtils.result(productDemandService.list(productDemandQueryList));
    }

    @ApiOperation("暂停或作废")
    @GetMapping("/updateStatus")
    public BusinessResult<Boolean> updateStatus(@RequestParam Long id,
                                                @ApiParam(value = "-10暂停,-20:作废", required = true)
                                                @RequestParam Integer type) {
        return ResultUtils.result(productDemandService.updateStatus(id, type));
    }

    @ApiOperation("产品需求开启")
    @GetMapping("/enable")
    public BusinessResult<Boolean> enable(@RequestParam Long id) {
        return ResultUtils.result(productDemandService.enable(id));
    }

    @ApiOperation("产品需求新增")
    @PostMapping("/add")
    public BusinessResult<Boolean> add(@RequestBody @Valid ProductDemandAddReq productDemandAddReq) {
        return ResultUtils.result(productDemandService.add(productDemandAddReq));
    }

    @ApiOperation("产品需求修改")
    @PostMapping("/modify")
    public BusinessResult<Boolean> modify(@RequestBody @Valid ProductDemandModifyReq productDemandModifyReq) {
        return ResultUtils.result(productDemandService.modify(productDemandModifyReq));
    }

    @ApiOperation("产品需求详情")
    @GetMapping("/get")
    public BusinessResult<ProductDemandDetailVO> get(@RequestParam Long id) {
        ProductDemandDetailVO demandDetailVO = productDemandService.get(id).getData();
        return ResultUtils.result(BaseResult.success(demandDetailVO));
    }

    @ApiOperation("匹配的项目列表")
    @PostMapping("/matchProjectList")
    public BusinessResult<PageQueryResult<ProjectVO>> matchProjectList(@RequestBody @Valid ProductDemandLinkProjectQueryList projectQueryList) {
        return ResultUtils.result(productDemandService.matchProjectList(projectQueryList));
    }

    @ApiOperation("匹配的业务需求列表")
    @PostMapping("/matchBizDemandList")
    public BusinessResult<PageQueryResult<BizDemandVO>> matchBizDemandList(@RequestBody @Valid ProductDemandLinkBizDemandQueryList productDemandLinkBizDemandQueryList) {
        return ResultUtils.result(productDemandService.matchBizDemandList(productDemandLinkBizDemandQueryList));
    }

    @ApiOperation("项目清单")
    @GetMapping("/linkProjectList")
    public BusinessResult<ProjectVO> linkProjectList(@RequestParam Long productDemandId) {
        return ResultUtils.result(BaseResult.success(productDemandService.linkProjectList(productDemandId)));
    }

    @ApiOperation("业务需求清单")
    @PostMapping("/linkBizDemandList")
    public BusinessResult<PageQueryResult<BizDemandVO>> linkBizDemandList(@RequestBody @Valid ProductBizDemandQueryList productBizDemandQueryList) {
        return ResultUtils.result(productDemandService.linkBizDemandList(productBizDemandQueryList));
    }

    @ApiOperation("关联/取消关联业务需求")
    @PostMapping("/linkOrUnLinkBizDemand")
    public BusinessResult<Boolean> linkOrUnLinkBizDemand(@RequestBody @Valid ProductBizDemandLinkReq bizDemandLinkReq) {
        return ResultUtils.result(productDemandService.linkOrUnLinkBizDemand(bizDemandLinkReq));
    }

    @ApiOperation("添加抄送人")
    @PostMapping("/addRecipients")
    public BusinessResult<Boolean> addRecipients(@RequestBody @Valid RecipientAddReq recipientAddReq) {
        return ResultUtils.result(personService.addRecipients(recipientAddReq));
    }

    @ApiOperation("批量转交")
    @PostMapping("/batchTransfer")
    public BusinessResult<Boolean> batchTransfer(@RequestBody @Valid BatchTransferReq batchTransferReq){
        return ResultUtils.result(productDemandService.productDemandBatchTransferReceiveMan(batchTransferReq));
    }

    @ApiOperation("匹配的埋点事件列表")
    @PostMapping("/matchTrackEventList")
    public BusinessResult<PageQueryResult<TrackEventVO>> matchTrackEventList(@RequestBody @Valid ProductDemandLinkTrackEventQueryList trackEventQueryList) {
        return ResultUtils.result(productDemandService.matchTrackEventList(trackEventQueryList));
    }

    @ApiOperation("埋点事件清单")
    @PostMapping("/linkTrackEventList")
    public BusinessResult<PageQueryResult<TrackEventVO>> linkTrackEventList(@RequestBody @Valid ProductDemandTrackEventQueryList trackEventQueryList) {
        return ResultUtils.result(productDemandService.linkTrackEventList(trackEventQueryList));
    }

    @ApiOperation("关联/取消关联埋点事件")
    @PostMapping("/linkOrUnLinkTrackEvent")
    public BusinessResult<Boolean> linkOrUnLinkTrackEvent(@RequestBody @Valid ProductDemandTrackEventLinkReq trackEventLinkReq) {
        return ResultUtils.result(productDemandService.linkOrUnLinkTrackEvent(trackEventLinkReq));
    }

    @ApiOperation("匹配的客户需求列表")
    @PostMapping("/matchCustomDemandList")
    public BusinessResult<PageQueryResult<CustomDemandVO>> matchCustomDemandList(@RequestBody @Valid ProductLinkCustomDemandQueryList customDemandQueryList) {
        return ResultUtils.result(productDemandService.matchCustomDemandList(customDemandQueryList));
    }

    @ApiOperation("客户需求清单")
    @PostMapping("/linkCustomDemandList")
    public BusinessResult<PageQueryResult<CustomDemandVO>> linkCustomDemandList(@RequestBody @Valid ProductCustomDemandQueryList customDemandQueryList) {
        return ResultUtils.result(productDemandService.linkCustomDemandList(customDemandQueryList));
    }

    @ApiOperation("关联/取消关联客户需求")
    @PostMapping("/linkOrUnLinkCustomDemand")
    public BusinessResult<Boolean> linkOrUnLinkCustomDemand(@RequestBody @Valid ProductCustomDemandLinkReq customDemandLinkReq) {
        return ResultUtils.result(productDemandService.linkOrUnLinkCustomDemand(customDemandLinkReq));
    }

    @ApiOperation("获取最新抄送人")
    @GetMapping("/getLastCopior")
    public BusinessResult<List<PersonVO>> getLastCopior() {
        return ResultUtils.result(personService.getLastCopior());
    }
}
