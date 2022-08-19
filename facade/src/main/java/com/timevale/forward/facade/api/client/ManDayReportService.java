package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ManDayQueryList;
import com.timevale.forward.facade.api.query.ManDayReportQueryList;
import com.timevale.forward.facade.api.query.ProjectManDayQueryList;
import com.timevale.forward.facade.api.request.ManDayModifyReq;
import com.timevale.forward.facade.api.request.ManDayReportBatchApproveReq;
import com.timevale.forward.facade.api.request.ManDayReportModifyReq;
import com.timevale.forward.facade.api.result.ManDayListVO;
import com.timevale.forward.facade.api.result.ManDayReportListVO;
import com.timevale.forward.facade.api.result.ProjectTotalManDayVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;


/**
 * 人天提报RPC接口
 *
 * @author by YangXu
 * @date 2022/08/19 15:58
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ManDayReportService {

    /**
     * 分页查询提报列表
     */
    BaseResult<PageQueryResult<ManDayReportListVO>> page(ManDayReportQueryList manDayReportQueryList);

    /**
     * 修改提报状态
     */
    BaseResult<Boolean> modify(ManDayReportModifyReq manDayReportModifyReq);

    /**
     * 批量同意
     */
    BaseResult<Boolean> batchApprove(ManDayReportBatchApproveReq manDayReportBatchApproveReq);

}
