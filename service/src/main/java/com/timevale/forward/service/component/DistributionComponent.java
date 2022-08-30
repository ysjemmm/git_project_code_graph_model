package com.timevale.forward.service.component;

import com.timevale.forward.dal.dto.UpdateTimeDTO;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/06/28 10:30
 */
public interface DistributionComponent {

    /**
     * 获取更新日期
     *
     * @return {@link UpdateTimeDTO}
     */
    UpdateTimeDTO getUpdateDate();
}
