package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.TrackImportService;
import com.timevale.forward.facade.api.request.TrackImportReq;
import com.timevale.forward.facade.api.result.TrackImportLogVO;
import com.timevale.forward.facade.api.result.TrackImportProgressVO;
import com.timevale.forward.facade.api.result.TrackImportTemplateVO;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

/**
 * @author by YangXu
 * @date 2022/08/08 17:18
 */
@Slf4j
@LogPoint
@RestService
public class TrackImportServiceImpl implements TrackImportService {
    @Override
    public BaseResult<Boolean> importEvent(TrackImportReq trackImportReq) {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> cancel() {
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<TrackImportProgressVO> progress() {
        return BaseResult.success(new TrackImportProgressVO());
    }

    @Override
    public BaseResult<TrackImportTemplateVO> template() {
        return BaseResult.success(new TrackImportTemplateVO());
    }

    @Override
    public BaseResult<PageQueryResult<TrackImportLogVO>> log() {
        return null;
    }
}
