package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningTaskDTO;
import com.timevale.forward.facade.api.request.HomePageBaseReq;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:48
 */
public interface HomePageRiskWarningTaskComponent {
    /**
     * 预警
     *
     * @return 列表
     */
    List<HomePageRiskWarningTaskDTO> getRiskWarningTask(HomePageBaseReq homePageBaseReq);
}
