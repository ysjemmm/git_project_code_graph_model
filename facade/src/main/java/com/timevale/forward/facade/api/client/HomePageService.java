package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.HomeTaskBoardQueryList;
import com.timevale.forward.facade.api.result.*;
import com.timevale.mandarin.common.annotation.RestClient;
import com.timevale.mandarin.common.result.PageQueryResult;

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
    BaseResult<HomeDataIndicatorVO> getDataIndicator();

    /**
     * 待办卡片
     *
     * @return 列表
     */
    BaseResult<HomeTodoCardVO> getTodoCard();


    /**
     * 近三周上线项目
     *
     * @return 列表
     */
    BaseResult<PageQueryResult<HomeProjectOnlineLatelyVO>> getProjectOnlineLately();


    /**
     * 待办卡片
     *
     * @return 列表
     */
    BaseResult<HomeRiskWarningVO> getRiskWarning();

    /**
     * 任务工时看板查询
     *
     * @return 列表
     */
    BaseResult<HomeTaskBoardVO> getHomeTaskBoard(HomeTaskBoardQueryList boardQueryList);

}
