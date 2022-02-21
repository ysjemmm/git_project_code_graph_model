package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;
import com.timevale.forward.facade.api.request.HomePageBaseReq;

/**
 * @author by YangXu
 * @date 2022/01/24 09:43
 */
public interface HomePageDataIndicatorComponent {
    /**
     * 查询数据指标
     * @param homePageBaseReq 查询条件
     * @return 数据指标VO
     */
    HomePageDataIndicatorDTO getDataIndicator(HomePageBaseReq homePageBaseReq);
}
