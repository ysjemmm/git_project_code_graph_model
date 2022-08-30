package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.DataModifyReq;
import com.timevale.forward.facade.api.request.ProjectNodeModifyReq;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface DataCorrectService {
    /**
     * 修改
     *
     * @param dataModifyReq dataModifyReq
     * @return Boolean
     */
    BaseResult<Boolean> modify(DataModifyReq dataModifyReq);

    /**
     * 修改
     *
     * @return Boolean
     */
    BaseResult<Boolean> calculateStatus();

    /**
     * 节点状态更新
     */
    BaseResult<Boolean> nodeStatusUpdate();

    /**
     * 业务需求项目发布时间更新
     */
    BaseResult<Boolean> bizDemandProjectEndDateUpdate();

    /**
     * 更新逾期天数
     * @return return
     */
    BaseResult<Boolean> updateDelayDays();

    /**
     * 节点时间更新
     * @param projectNodeModifyReq projectNodeModifyReq
     * @return
     */
    BaseResult<Boolean> updateNodeDate(ProjectNodeModifyReq projectNodeModifyReq);


    /**
     * 更新逾期天数
     * @return return
     */
    BaseResult<Boolean> updateModelIdInBugOnline();
}
