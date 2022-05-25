package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.ProjectBoardReq;
import com.timevale.forward.facade.api.result.ProjectBoardDataIndicatorVO;
import com.timevale.mandarin.common.annotation.RestClient;


/**
 * @author by YangXu
 * @date 2022/05/25 16:43
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectBoardService {

    /**
     * 获取项目数据指标
     *
     * @param projectBoardReq 项目数据看板
     */
    BaseResult<ProjectBoardDataIndicatorVO> getDataIndicator(ProjectBoardReq projectBoardReq);
}
