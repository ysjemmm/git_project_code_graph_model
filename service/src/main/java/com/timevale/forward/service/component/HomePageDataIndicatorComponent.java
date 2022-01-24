package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;

/**
 * @author by YangXu
 * @date 2022/01/24 09:43
 */
public interface HomePageDataIndicatorComponent {
    /**
     * 查询数据指标
     *
     * @return 数据指标VO
     */
    HomePageDataIndicatorDTO getDataIndicator(String userType);
}
