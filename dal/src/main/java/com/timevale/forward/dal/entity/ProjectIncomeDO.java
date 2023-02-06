package com.timevale.forward.dal.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目收益表
 * @TableName project_income
 */
@Data
public class ProjectIncomeDO extends BaseDO{
    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 收益金额（元）
     */
    private BigDecimal incomeAmount;

    /**
     * 收益日期
     */
    private Date incomeDate;

    /**
     * 收益情况
     */
    private String situation;

}