package com.timevale.forward.service.impl;

import com.timevale.forward.facade.api.client.BugOnlineService;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BugOnlineDetailVO;
import com.timevale.forward.facade.api.result.BugOnlineVO;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Date 2022/3/17 16:59
 * @Author 望轩
 */
@Slf4j
@RestService
public class BugOnlineServiceImpl implements BugOnlineService {

    @Override
    public BusinessResult<List<String>> getAllDisplayField(BugOnlineGetFieldReq bugOnlineGetFieldReq) {
        return null;
    }

    @Override
    public BusinessResult<PageQueryResult<BugOnlineVO>> list(BugOnlineQueryList bugOnlineQueryList) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> add(BugOnlineAddReq bugOnlineAddReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> modify(BugOnlineModifyReq bugOnlineModifyReq) {
        return null;
    }

    @Override
    public BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq bugOnlineDetailReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> confirm(BugOnlineReq bugOnlineReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> startRepair(BugOnlineReq bugOnlineReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> repairFinished(BugOnlineRepairFinishedReq bugOnlineRepairFinishedReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> confirmRepair(BugOnlineConfirmRepairReq bugOnlineConfirmRepairReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> online(BugOnlineOnlineReq bugOnlineOnlineReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> openAgain(BugOnlineOpenAgainReq bugOnlineOpenAgainReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> noRepair(BugOnlineNoRepairReq bugOnlineNoRepairReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> transfer(BugOnlineTransferReq bugOnlineTransferReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> agree(BugOnlineReq bugOnlineReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> reject(BugOnlineReq bugOnlineReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> reconfirm(BugOnlineReq bugOnlineReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> temporaryNoRepair(BugOnlineReq bugOnlineReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> repairFailed(BugOnlineRepairFailedReasonReq bugOnlineRepairFailedReasonReq) {
        return null;
    }
}