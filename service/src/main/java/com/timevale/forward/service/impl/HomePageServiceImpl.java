package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.HomePageService;
import com.timevale.forward.facade.api.query.HomePageTaskBoardQueryList;
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
    public BaseResult<HomePageDataIndicatorVO> getDataIndicator() {
        HomePageDataIndicatorVO dataIndicatorVO=new HomePageDataIndicatorVO();
        return BaseResult.success(dataIndicatorVO);
    }

    @Override
    public BaseResult<HomePageTodoCardVO> getTodoCard() {
        HomePageTodoCardVO todoCardVO=new HomePageTodoCardVO();
        return BaseResult.success(todoCardVO);
    }

    @Override
    public BaseResult<PageQueryResult<HomePageProjectOnlineLatelyVO>> getProjectOnlineLately() {
        PageQueryResult<HomePageProjectOnlineLatelyVO> pageQueryResult = new PageQueryResult<>();
//        ProjectOnlineLatelyVO projectOnlineLatelyVO=new ProjectOnlineLatelyVO();
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<HomePageRiskWarningVO> getRiskWarning() {
        HomePageRiskWarningVO riskWarningVO=new HomePageRiskWarningVO();
        return BaseResult.success(riskWarningVO);
    }

    @Override
    public BaseResult<HomePageTaskBoardVO> getTaskBoard(HomePageTaskBoardQueryList boardQueryList) {
        HomePageTaskBoardVO taskBoardVO=new HomePageTaskBoardVO();
        return BaseResult.success(taskBoardVO);
    }
}
