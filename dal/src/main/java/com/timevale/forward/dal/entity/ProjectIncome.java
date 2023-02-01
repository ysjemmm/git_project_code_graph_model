package com.timevale.forward.dal.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目收益表
 * @TableName project_income
 */
@Data
public class ProjectIncome {
    /**
     * 主键id
     */
    private Long id;

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