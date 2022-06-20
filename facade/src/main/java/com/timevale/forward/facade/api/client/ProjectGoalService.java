package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectGoalAddReq;
import com.timevale.forward.facade.api.request.ProjectGoalFinishReq;
import com.timevale.forward.facade.api.request.ProjectGoalIdReq;
import com.timevale.forward.facade.api.request.ProjectGoalModifyReq;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author jingchun
 * create on 2022/6/20
 */

@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectGoalService {

    /**
     * 新增项目目标
     */
    BaseResult<Boolean> add(ProjectGoalAddReq projectGoalAddReq);

    /**
     * 更新项目目标
     */
    BaseResult<Boolean> modify(ProjectGoalModifyReq projectGoalModifyReq);

    /**
     * 删除项目目标
     */
    BaseResult<Boolean> delete(ProjectGoalIdReq projectGoalIdReq);

    /**
     * 设置主目标
     */
    BaseResult<Boolean> setMainGoal(ProjectGoalIdReq projectGoalIdReq);

    /**
     * 完成情况填写
     */
    BaseResult<Boolean> finish(ProjectGoalFinishReq projectGoalFinishReq);

}
