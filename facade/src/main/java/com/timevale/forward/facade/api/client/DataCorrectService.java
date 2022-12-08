package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectNodeModifyReq;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface DataCorrectService {

    /**
     * 节点状态更新
     */
    BaseResult<Boolean> nodeStatusUpdate();

    /**
     * 业务需求项目发布时间更新
     */
    BaseResult<Boolean> bizDemandProjectEndDateUpdate();
    /**
     * 节点时间更新
     * @param req req
     * @return
     */
    BaseResult<Boolean> updateNodeDate(ProjectNodeModifyReq req);

    /**
     * 故障单刷新持续时间
     *
     * @return {@link BaseResult}<{@link Boolean}>
     */
    BaseResult<Boolean> troubleTicketTime();

    /**
     * 线上bug 关闭状态操作人初始化
     * @param count
     * @return
     */
    BaseResult<Boolean> bugOnlineCloseStatusOperatorInit(Integer count);
}
