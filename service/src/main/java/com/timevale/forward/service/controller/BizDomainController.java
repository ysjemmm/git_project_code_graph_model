package com.timevale.forward.service.controller;

import com.timevale.forward.facade.api.client.BizDomainService;
import com.timevale.forward.facade.api.query.BizDomainQueryList;
import com.timevale.forward.facade.api.request.BizDomainAddReq;
import com.timevale.forward.facade.api.request.BizDomainModifyReq;
import com.timevale.forward.facade.api.request.UpdateBizDomainListingStatusReq;
import com.timevale.forward.facade.api.result.BizDomainVO;
import com.timevale.forward.service.utils.ResultUtils;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author by YangXu
 * @Date 2021/12/13
 */
@RestController
@Api(tags = "业务域管理")
@Slf4j
@RequestMapping("/forward/bizDomain")
public class BizDomainController {

    @Resource
    private BizDomainService bizDomainService;

    @ApiOperation("业务域列表(owner排序)")
    @PostMapping("/bizDomainListWithOrder")
    public BusinessResult<PageQueryResult<BizDomainVO>> bizDomainListWithOrder(@RequestBody @Valid BizDomainQueryList bizDomainQueryList) {
        return ResultUtils.result(bizDomainService.bizDomainListWithOrder(bizDomainQueryList));
    }

    @ApiOperation("业务域列表")
    @GetMapping("/list")
    public BusinessResult<List<BizDomainVO>> list() {
        return ResultUtils.result(bizDomainService.bizDomainList());
    }

    @ApiOperation("业务域列表(带条件搜索)")
    @PostMapping("/bizDomainList")
    public BusinessResult<PageQueryResult<BizDomainVO>> list(BizDomainQueryList bizDomainQueryList) {
        return ResultUtils.result(bizDomainService.bizDomainList(bizDomainQueryList));
    }

    @ApiOperation("新增")
    @PostMapping("/add")
    public BusinessResult<Boolean> add(@RequestBody @Valid BizDomainAddReq bizDomainAddReq) {
        return ResultUtils.result(bizDomainService.add(bizDomainAddReq));
    }

    @ApiOperation("编辑")
    @PostMapping("/update")
    public BusinessResult<Boolean> update(@RequestBody @Valid BizDomainModifyReq bizDomainModifyReq) {
        return ResultUtils.result(bizDomainService.update(bizDomainModifyReq));
    }


    @ApiOperation("修改业务域上架状态")
    @PostMapping("/updateListingStatus")
    public BusinessResult<Boolean> updateListingStatus(@RequestBody @Valid UpdateBizDomainListingStatusReq req) {
        return ResultUtils.result(bizDomainService.updateBizDomainListingStatus(req));
    }

    @ApiOperation("删除业务域")
    @GetMapping("/delete")
    public BusinessResult<Boolean> delete(
            @ApiParam(value = "业务域ID", required = true) @RequestParam(value = "bizDomainId") Long bizDomainId) {
        return ResultUtils.result(bizDomainService.deleteBizDomain(bizDomainId));
    }

}
