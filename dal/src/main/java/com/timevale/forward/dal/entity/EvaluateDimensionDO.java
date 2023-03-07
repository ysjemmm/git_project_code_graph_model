package com.timevale.forward.dal.entity;

import lombok.Data;

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

    private Integer scoresCeiling;

    private Integer scoresFloor;
}