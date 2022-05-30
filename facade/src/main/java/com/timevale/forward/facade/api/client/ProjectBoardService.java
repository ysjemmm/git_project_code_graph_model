package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.result.BugOfflineTrendVO;
import com.timevale.forward.facade.api.result.ProjectBoardDataIndicatorVO;
import com.timevale.forward.facade.api.result.ProjectBoardSinglelWorkTimeVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;


/**
 * @author by YangXu
 * @date 2022/05/25 16:43
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface ProjectBoardService {

    /**
     * 获取项目数据指标
     *
     * @param projectId 项目id
     */
    BaseResult<ProjectBoardDataIndicatorVO> getDataIndicator(Long projectId);


    /**
     * 获取线下bug趋势图
     *
     * @param projectId 项目id
     */
    BaseResult<List<BugOfflineTrendVO>> getBoardBugOfflineTrend(Long projectId);


    /**
     * 获取项目人员工期
     *
     * @param projectId 项目id
     */
    BaseResult<List<ProjectBoardSinglelWorkTimeVO>> getWorkTime(Long projectId);
}
