package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.query.HomeTaskBoardQueryList;
import com.timevale.forward.facade.api.result.*;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

/**
 * @author by YangXu
 * @date 2022/01/11 17:43
 */
@Slf4j
@RestService
public class HomePageServiceImpl implements HomePageService {

    @Override
    public BaseResult<HomeDataIndicatorVO> getDataIndicator() {
        HomeDataIndicatorVO dataIndicatorVO=new HomeDataIndicatorVO();
        return BaseResult.success(dataIndicatorVO);
    }

    @Override
    public BaseResult<HomeTodoCardVO> getTodoCard() {
        HomeTodoCardVO todoCardVO=new HomeTodoCardVO();
        return BaseResult.success(todoCardVO);
    }

    @Override
    public BaseResult<PageQueryResult<HomeProjectOnlineLatelyVO>> getProjectOnlineLately() {
        PageQueryResult<HomeProjectOnlineLatelyVO> pageQueryResult = new PageQueryResult<>();
//        ProjectOnlineLatelyVO projectOnlineLatelyVO=new ProjectOnlineLatelyVO();
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<HomeRiskWarningVO> getRiskWarning() {
        HomeRiskWarningVO riskWarningVO=new HomeRiskWarningVO();
        return BaseResult.success(riskWarningVO);
    }

    @Override
    public BaseResult<HomeTaskBoardVO> getHomeTaskBoard(HomeTaskBoardQueryList boardQueryList) {
        HomeTaskBoardVO homeTaskBoardVO=new HomeTaskBoardVO();
        return BaseResult.success(homeTaskBoardVO);
    }
}
