package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.result.HomePageRiskWarningVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 09:48
 */
public interface HomePageRiskWarningComponent {
    /**
     * 预警
     *
     * @return 列表
     */
    List<HomePageRiskWarningDTO> getRiskWarning();
}
