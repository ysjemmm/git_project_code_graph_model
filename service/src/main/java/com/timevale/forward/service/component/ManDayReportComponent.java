package com.timevale.forward.service.component;

import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2022/08/22 10:17
 */
public interface ManDayReportComponent {
    /**
     * 添加
     *
     * @param manDayId    人天id
     * @param auditManDay 审核人天
     */
    void add(Long manDayId, BigDecimal auditManDay);

    /**
     * 批量通知
     *
     * @param projectId 项目id
     */
    void batchMsg(Long projectId);

    /**
     * 更新审批人
     *
     * @param projectId 项目id
     */
    void updateAuditor(Long projectId);
}
