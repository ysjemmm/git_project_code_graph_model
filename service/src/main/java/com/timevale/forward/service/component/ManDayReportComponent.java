package com.timevale.forward.service.component;

import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2022/08/22 10:17
 */
public interface ManDayReportComponent {
    void add(Long manDayId, BigDecimal auditManDay);
}
