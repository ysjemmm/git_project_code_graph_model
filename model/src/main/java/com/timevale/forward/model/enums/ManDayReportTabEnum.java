package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/08/19 18:23
 */
@Getter
@AllArgsConstructor
public enum ManDayReportTabEnum {
    /**
     * 待我审核的
     */
    AUDIT,
    /**
     * 我提报的人天
     */
    REPORT,
    /**
     * 全部人天
     */
    All
}
