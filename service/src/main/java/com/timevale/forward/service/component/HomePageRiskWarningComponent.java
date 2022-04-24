package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.facade.api.request.HomePageBaseReq;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:48
 */
public interface HomePageRiskWarningComponent {
    /**
     * 预警
     *
     * @param homePageBaseReq 查询类型
     * @return 列表
     */
    List<HomePageRiskWarningDTO> getRiskWarning(HomePageBaseReq homePageBaseReq);

    /**
     * 查询全部风险预警
     *
     * @return 列表
     */
    List<HomePageRiskWarningDTO> getRiskWarningAll();
}
