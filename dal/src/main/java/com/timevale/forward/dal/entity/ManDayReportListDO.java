package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 人天提报记录表
 */

@Data
@EqualsAndHashCode(callSuper = true)
public class ManDayReportListDO extends BaseDO {
    private Long id;

    private String projectName;

    private Date weekStartDate;

    private Date weekEndDate;

    private String pm;

    private String pmId;

    private Date createDate;

    private String createMan;

    private String createManId;

    private BigDecimal auditManDay;

    private Integer auditStatus;
}