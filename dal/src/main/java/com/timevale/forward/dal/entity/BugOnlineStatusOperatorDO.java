package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Description: 线上bug状态操作记录表
 * @ClassName: BugOnlineStatusOperatorDO
 * @Author yexuan
 * @Date  2022-12-08 14:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOnlineStatusOperatorDO extends BaseDO {
    /**
     * bug 线上 id
     */
    private Long bugOnlineId;

    /**
     * 经办人
     */
    private String operator;

    /**
     * 经办人id
     */
    private String operatorId;

    /**
     * 状态
     */
    private Integer status;
}