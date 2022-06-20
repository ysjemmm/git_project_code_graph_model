package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 人天记录表
 */
@Getter
@Setter
@NoArgsConstructor
public class ManDayDO extends BaseDO {
    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 项目成员id
     */
    private String memberId;

    /**
     * 项目成员名
     */
    private String memberName;

    /**
     * 实际人天
     */
    private BigDecimal actualManDay;

    /**
     * 周开始日期
     */
    private Date weekStartDate;

    /**
     * 周结束日期
     */
    private Date weekEndDate;

}