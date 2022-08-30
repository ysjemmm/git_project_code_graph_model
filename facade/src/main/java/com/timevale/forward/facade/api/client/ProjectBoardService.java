package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.ProjectBugOfflineCountQueryList;
import com.timevale.forward.facade.api.query.TaskOverdueRankQueryList;
import com.timevale.forward.facade.api.result.*;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

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

    /**
     * 项目人员任务逾期时间排行
     */
    BaseResult<PageQueryResult<TaskOverdueCountVO>> getTaskOverdueRank(TaskOverdueRankQueryList query);

    /**
     * 待修复线下bug情况
     */
    BaseResult<PageQueryResult<BugOfflineCountVO>> getBugOfflineCount(ProjectBugOfflineCountQueryList query);

    /**
     * 待修复线下bug情况(总数)
     */
    BaseResult<BugOfflineAllCountVO> getBugOfflineAllCount(Long projectId);

    /**
     * 线下Bug原因分布情况
     * @param projectId 项目id
     * @return 线下BUG原因分布情况列表
     */
    BaseResult<List<BugOfflineReasonDistributionVO>> getProjectBugReasonDistribution(Long projectId);

    /**
     * 线下Bug所属端分布情况
     * @param projectId 项目id
     * @return 线下BUG所属端分布情况列表
     */
    BaseResult<List<BugOfflineBelongDistributionVO>> getProjectBugBelongDistribution(Long projectId);

}
