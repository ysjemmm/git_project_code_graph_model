package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.request.HomePageBaseReq;
import com.timevale.forward.facade.api.request.HomePageHolidayReq;
import com.timevale.forward.facade.api.request.HomePageProjectBoardReq;
import com.timevale.forward.facade.api.request.HomePageTaskBoardReq;
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
     * @param homePageProjectBoardReq 查询条件
     * @return 列表
     */
    BaseResult<List<HomePageProjectBoardVO>> getProjectBoard(HomePageProjectBoardReq homePageProjectBoardReq);

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
     * 团队任务工时看板
     */
    BaseResult<List<HomePageGroupWorkTimeVO>> getGroupTaskWorkTimeBoard(HomePageTaskBoardReq req);

    /**
     *
     * @param homePageHolidayReq  homePageHolidayReq
     * @return 节假日期
     */
    BaseResult<List<String>> getHolidays(HomePageHolidayReq homePageHolidayReq);
}
