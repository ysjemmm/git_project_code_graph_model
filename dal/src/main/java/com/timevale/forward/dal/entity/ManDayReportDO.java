package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 人天提报记录表
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class ManDayReportDO extends BaseDO {
    /**
     * 人天id
     */
    private Long manDayId;

    /**
     * 审计人天
     */
    private BigDecimal auditManDay;

    /**
     * 工时说明
     */
    private String manDayDesc;

    /**
     * 审计状态
     */
    private Integer auditStatus;

    /**
     * 驳回理由
     */
    private String rejectReason;

    /**
     * 审核员
     */
    private String auditor;

    /**
     * 审核员id
     */
    private String auditorId;

    /**
     * 提报人
     */
    private String reportor;

    /**
     * 提报人id
     */
    private String reportorId;
}