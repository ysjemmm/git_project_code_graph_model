package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.query.HomePageProjectBoardQueryList;
import com.timevale.forward.facade.api.result.HomePageProjectBoardVO;
import com.timevale.mandarin.common.annotation.RestClient;

/**
 * @author by YangXu
 * @date 2022/01/24 09:48
 */
public interface HomePageProjectBoardComponent {
    /**
     * 项目工时看板查询
     *
     * @param boardQueryList 查询条件
     * @return VO
     */
    BaseResult<HomePageProjectBoardVO> getProjectBoard(HomePageProjectBoardQueryList boardQueryList);
}
