package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/11 17:42
 */
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface HomePageService {

    /**
     * 查数据指标
     *
     * @param homePageBaseReq 查询条件
     * @return 列表
     */
    BaseResult<HomePageDataIndicatorVO> getDataIndicator(HomePageBaseReq homePageBaseReq);

    /**
     * 待办卡片
     *
     * @param homePageBaseReq 查询条件
     * @return 列表
     */
    BaseResult<HomePageTodoCardVO> getTodoCard(HomePageBaseReq homePageBaseReq);


    /**
     * 近三周上线项目
     *
     * @param homePageProjectOnlineLatelyQueryList 查询条件
     * @return 列表
     */
    BaseResult<PageQueryResult<HomePageProjectOnlineLatelyVO>> getProjectOnlineLately(HomePageProjectOnlineLatelyQueryList homePageProjectOnlineLatelyQueryList);


    /**
     * 预警
     *
     * @param homePageBaseReq 查询条件
     * @return 列表
     */
    BaseResult<List<HomePageRiskWarningVO>> getRiskWarning(HomePageBaseReq homePageBaseReq);

    /**
     * 项目工时看板查询
     *
     * @param req 查询条件
     * @return 列表
     */
    BaseResult<List<HomePageProjectBoardVO>> getProjectBoard(HomePageProjectBoardReq req);

    /**
     * 获取更新时间
     *
     * @return {@link BaseResult}<{@link UpdateTimeVO}>
     */
    BaseResult<UpdateTimeVO> getUpdateTime();


    /**
     * 任务工时看板查询
     *
     * @param req 查询条件
     * @return 列表
     */
    BaseResult<List<HomePageSingleWorkTimeVO>> getTaskWorkTimeBoard(HomePageTaskBoardReq req);

    /**
     * 任务工时看板查询（新）
     *
     * @param req 查询条件
     * @return 列表
     */
    BaseResult<List<ProjectBoardSinglelWorkTimeVO>> getWorkTime(HomePageTaskBoardReq req);

    /**
     * 团队任务工时看板
     */
    BaseResult<List<HomePageGroupWorkTimeVO>> getGroupTaskWorkTimeBoard(HomePageTaskBoardReq req);

    /**
     * @param homePageHolidayReq  homePageHolidayReq
     * @return 节假日期
     */
    BaseResult<List<String>> getHolidays(HomePageHolidayReq homePageHolidayReq);

    /**
     * 获取快捷搜索条件
     *
     * @return 快捷搜索条件列表
     */
    BaseResult<List<FastSearchConditionVO>> getFastSearchConditions();

    /**
     * 删除快捷搜索条件
     */
    BaseResult<Boolean> deleteFastSearchCondition(DeleteFastSearchConditionReq req);

}
