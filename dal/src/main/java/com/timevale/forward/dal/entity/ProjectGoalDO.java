package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@NoArgsConstructor
public class ProjectGoalDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 名称
     */
    private String name;

    /**
     * 目标性质: 0定量,1定性
     */
    private Integer type;

    /**
     * 是否主目标: 0否,1是
     */
    private Integer isMain;

    /**
     * 0:进行中;10:已完成;30:未完成
     */
    private Integer status;

    /**
     * 目标衡量标准
     */
    private String measurement;

    /**
     * 项目目标达标值
     */
    private BigDecimal reachValue;

    /**
     * 项目目标达成日期
     */
    private Date reachDate;

    /**
     * 完成情况说明
     */
    private String completeNote;

}
