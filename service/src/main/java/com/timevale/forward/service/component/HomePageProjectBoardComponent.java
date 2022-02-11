package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageProjectBoardDTO;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:48
 */
public interface HomePageProjectBoardComponent {
    /**
     * 项目工时看板查询
     *
     * @param userIdList 用户idList
     * @return VO
     */
    List<HomePageProjectBoardDTO> getProjectBoard(List<String> userIdList);
}
