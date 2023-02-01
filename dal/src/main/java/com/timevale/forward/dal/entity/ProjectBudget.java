package com.timevale.forward.dal.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目预算表
 * @TableName project_budget
 */
@Data
public class ProjectBudget {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 成本类型
     */
    private String costType;

    /**
     * 预计成本金额（元）
     */
    private BigDecimal expectedAmount;

    /**
     * 实际已发生成本金额（元）
     */
    private BigDecimal costAmount;

    /**
     * 预计成本说明
     */
    private String expectedDesc;

    /**
     * 执行金额说明
     */
    private String costDesc;

    /**
     * 发生日期
     */
    private Date costDate;

    /**
     * 删除标记
     */
    private Integer isDeleted;

    /**
     * 创建人id
     */
    private String createManId;

    /**
     * 创建人
     */
    private String createMan;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 修改人id
     */
    private String modifyManId;

    /**
     * 修改人
     */
    private String modifyMan;

    /**
     * 修改时间
     */
    private Date modifyDate;

}