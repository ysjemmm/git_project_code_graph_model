package com.timevale.forward.service.component.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.query.HomePageProjectBoardQueryList;
import com.timevale.forward.facade.api.result.HomePageProjectBoardVO;
import com.timevale.forward.service.component.HomePageProjectBoardComponent;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2022/01/24 10:09
 */
@Slf4j
@Component
public class HomePageProjectBoardComponentImpl extends BaseDistributeClientImpl<HomePageProjectBoardVO> implements HomePageProjectBoardComponent {
    @Override
    public BaseResult<HomePageProjectBoardVO> getProjectBoard(HomePageProjectBoardQueryList boardQueryList) {
        return null;
    }
}
