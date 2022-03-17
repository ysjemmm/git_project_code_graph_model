package com.timevale.forward.service.impl;

import com.timevale.forward.facade.api.client.BugOnlineService;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import com.timevale.forward.facade.api.result.BugOnlineVO;
import com.timevale.forward.facade.api.result.ProductLineToFieldVO;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

/**
 * @Date 2022/3/17 16:59
 * @Author 望轩
 */
@Slf4j
@RestService
public class BugOnlineServiceImpl implements BugOnlineService {

    @Override
    public BusinessResult<ProductLineToFieldVO> getAllDisplayField(BugOnlineGetFieldReq bugOnlineGetFieldReq) {
        BusinessResult<ProductLineToFieldVO> businessResult = new BusinessResult<>();
        return businessResult;
    }

    @Override
    public BusinessResult<PageQueryResult<BugOnlineVO>> list(BugOnlineQueryList bugOnlineQueryList) {
        BusinessResult<PageQueryResult<BugOnlineVO>> businessResult = new BusinessResult<>();
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> add(BugOnlineAddReq bugOnlineAddReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> modify(BugOnlineModifyReq bugOnlineModifyReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq bugOnlineDetailReq) {
        BusinessResult<BugOnlineDetailVO> businessResult = new BusinessResult<>();
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> confirm(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> startRepair(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> repairFinished(BugOnlineRepairFinishedReq bugOnlineRepairFinishedReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> confirmRepair(BugOnlineConfirmRepairReq bugOnlineConfirmRepairReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> online(BugOnlineOnlineReq bugOnlineOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> openAgain(BugOnlineOpenAgainReq bugOnlineOpenAgainReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> noRepair(BugOnlineNoRepairReq bugOnlineNoRepairReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> transfer(BugOnlineTransferReq bugOnlineTransferReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> agree(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> reject(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> reconfirm(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> temporaryNoRepair(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> repairFailed(BugOnlineRepairFailedReasonReq bugOnlineRepairFailedReasonReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }
}