package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:48
 */
public interface HomePageRiskWarningComponent {
    /**
     * 预警
     *
     * @param userType 用户类型
     * @return 列表
     */
    List<HomePageRiskWarningDTO> getRiskWarning(String userType);
}
