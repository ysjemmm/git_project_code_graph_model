package com.timevale.forward.service.impl;

import com.timevale.forward.facade.api.client.ImprovementMeasureService;
import com.timevale.forward.facade.api.query.ImprovementMeasureQueryList;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureDeleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureModifyReq;
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
public class ImprovementMeasureServiceImpl implements ImprovementMeasureService {

    @Override
    public BusinessResult<Boolean> add(ImprovementMeasureAddReq improvementMeasureAddReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> modify(ImprovementMeasureModifyReq improvementMeasureModifyReq) {
        return null;
    }

    @Override
    public BusinessResult<Boolean> delete(ImprovementMeasureDeleteReq improvementMeasureDeleteReq) {
        return null;
    }

    @Override
    public BusinessResult<List<TroubleTicketVO>> list(ImprovementMeasureQueryList improvementMeasureQueryList) {
        return null;
    }
}
