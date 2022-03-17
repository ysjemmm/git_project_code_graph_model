package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ImprovementMeasureQueryList;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureCompleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureDeleteReq;
import com.timevale.forward.facade.api.request.ImprovementMeasureModifyReq;
import com.timevale.forward.facade.api.result.TroubleTicketVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/03/16 17:51
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ImprovementMeasureService {

    /**
     * 改进措施-新增
     *
     * @param improvementMeasureAddReq 改进措施添加请求
     * @return 成功与否
     */
    BaseResult<Boolean> add(ImprovementMeasureAddReq improvementMeasureAddReq);

    /**
     * 改进措施-修改
     *
     * @param improvementMeasureModifyReq 改进措施修改请求
     * @return 成功与否
     */
    BaseResult<Boolean> modify(ImprovementMeasureModifyReq improvementMeasureModifyReq);

    /**
     * 改进措施-删除
     *
     * @param improvementMeasureDeleteReq 改进措施删除请求
     * @return 成功与否
     */
    BaseResult<Boolean> delete(ImprovementMeasureDeleteReq improvementMeasureDeleteReq);

    /**
     * 改进措施-完成
     *
     * @param improvementMeasureCompleteReq 改进措施删除请求
     * @return 成功与否
     */
    BaseResult<Boolean> complete(ImprovementMeasureCompleteReq improvementMeasureCompleteReq);

    /**
     * 改进措施-列表查询
     *
     * @param improvementMeasureQueryList 改进措施列表查询条件
     * @return 改进措施列表
     */
    BaseResult<List<TroubleTicketVO>> list(ImprovementMeasureQueryList improvementMeasureQueryList);

}
