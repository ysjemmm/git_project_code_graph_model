package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Date 2022/3/18 11:48
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOnlineModelDO extends BaseDO {
    /**
     * 线上bug id
     */
    private Long bugOnlineId;

    /**
     * 模块id
     */
    private Long modelId;
}