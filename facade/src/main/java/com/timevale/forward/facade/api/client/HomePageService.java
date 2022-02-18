package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.facade.api.request.HomePageProjectBoardReq;
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
     * @return 列表
     */
    BaseResult<HomePageDataIndicatorVO> getDataIndicator();

    /**
     * 待办卡片
     *
     * @param userType 用户类型
     * @return 列表
     */
    BaseResult<HomePageTodoCardVO> getTodoCard(String userType);


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
     * @param userType 用户类型
     * @return 列表
     */
    BaseResult<List<HomePageRiskWarningVO>> getRiskWarning(String userType);

    /**
     * 项目工时看板查询
     *
     * @param homePageProjectBoardReq 查询条件
     * @return 列表
     */
    BaseResult<List<HomePageProjectBoardVO>> getProjectBoard(HomePageProjectBoardReq homePageProjectBoardReq);

}
