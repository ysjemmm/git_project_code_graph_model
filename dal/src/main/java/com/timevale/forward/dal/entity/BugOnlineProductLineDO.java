package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Date 2022/3/18 11:48
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOnlineProductLineDO extends BaseDO {
    /**
     * 线上bug id
     */
    private Long bugOnlineId;

    /**
     * 产品线id
     */
    private Long productLineId;
}