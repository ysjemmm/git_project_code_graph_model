package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 人天记录表
 */
@Getter
@Setter
@Accessors(chain = true)
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
     * 人天填报说明
     */
    private String manDayDesc;

    /**
     * 周开始日期
     */
    private Date weekStartDate;

    /**
     * 周结束日期
     */
    private Date weekEndDate;

    /**
     * 审核状态
     */
    private Integer auditStatus;

    /**
     * 审核人天
     */
    private BigDecimal auditManDay;

    /**
     * 拒绝理由
     */
    private String rejectReason;
}