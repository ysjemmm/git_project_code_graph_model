package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageRiskWarningSubmitTestDTO;
import com.timevale.forward.facade.api.request.HomePageBaseReq;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:48
 */
public interface HomePageRiskWarningSubmitTestComponent {
    /**
     * 预警
     *
     * @return 列表
     */
    List<HomePageRiskWarningSubmitTestDTO> getRiskWarningSubmitTest(HomePageBaseReq homePageBaseReq);

    /**
     * 预警 全部
     */
    List<HomePageRiskWarningSubmitTestDTO> getRiskWarningSubmitTestAll();
}
