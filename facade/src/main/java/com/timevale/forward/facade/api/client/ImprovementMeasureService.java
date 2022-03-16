package com.timevale.forward.facade.api.client;

import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ImprovementMeasureQueryList;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.TroubleTicketDetailVO;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.BusinessResult;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/03/16 17:51
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ImprovementMeasureService {

    /**
     * 新增改进措施
     *
     * @param improvementMeasureAddReq 改进措施添加请求
     * @return 成功与否
     */
    BusinessResult<Boolean> add(ImprovementMeasureAddReq improvementMeasureAddReq);

    /**
     * 修改改进措施
     *
     * @param improvementMeasureModifyReq 改进措施修改请求
     * @return 成功与否
     */
    BusinessResult<Boolean> modify(ImprovementMeasureModifyReq improvementMeasureModifyReq);

    /**
     * 删除改进措施
     *
     * @param improvementMeasureDeleteReq 改进措施删除请求
     * @return 成功与否
     */
    BusinessResult<Boolean> delete(ImprovementMeasureDeleteReq improvementMeasureDeleteReq);

    /**
     * 改进措施列表查询
     *
     * @param improvementMeasureQueryList 改进措施列表查询条件
     * @return 改进措施列表
     */
    BusinessResult<List<TroubleTicketVO>> list(ImprovementMeasureQueryList improvementMeasureQueryList);

}
