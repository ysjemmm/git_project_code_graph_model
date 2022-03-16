package com.timevale.forward.service.impl;

import com.timevale.forward.facade.api.client.TroubleTicketService;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.TroubleTicketAddReq;
import com.timevale.forward.facade.api.request.TroubleTicketDeleteReq;
import com.timevale.forward.facade.api.request.TroubleTicketModifyReq;
import com.timevale.forward.facade.api.result.TroubleTicketDetailVO;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.BusinessResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/03/16 17:54
 */
@Slf4j
@RestService
public class TroubleTicketServiceImpl implements TroubleTicketService {

    @Override
    public BusinessResult<Boolean> add(TroubleTicketAddReq troubleTicketAddReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> modify(TroubleTicketModifyReq troubleTicketModifyReq) {
        return null;
    }

    @Override
    public BusinessResult<TroubleTicketDetailVO> get(Long troubleTicketId) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> delete(TroubleTicketDeleteReq troubleTicketDeleteReq) {
        return null;
    }

    @Override
    public BusinessResult<List<TroubleTicketVO>> list(TroubleTicketQueryList troubleTicketQueryList) {
        return null;
    }
}
