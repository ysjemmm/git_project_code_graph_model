package com.timevale.forward.dal.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @TableName evaluate_dimension
 */
@Data
public class EvaluateDimensionDO extends BaseDO {
    private Integer kind;

    private Date effectDate;

    private String dimensionName;

    private String scoresExplain;

    private BigDecimal scoresCeiling;

    private BigDecimal scoresFloor;
}