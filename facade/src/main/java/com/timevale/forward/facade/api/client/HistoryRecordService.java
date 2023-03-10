package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.HistoryRecordCmpReq;
import com.timevale.forward.facade.api.result.HistoryRecordCmpVO;
import com.timevale.forward.facade.api.result.HistoryRecordVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;


/**
 * @author by YangXu
 * @date 2023/03/10 19:38
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface HistoryRecordService {

    BaseResult<List<HistoryRecordVO>> list(Long id);

    BaseResult<List<HistoryRecordCmpVO>> compare(HistoryRecordCmpReq req);
}
