package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Date 2022/3/18 18:15
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugStatusOperatorDO extends BaseDO {
    /**
     * bug变更id
     */
    private Long bugLogId;

    /**
     * 经办人
     */
    private String operator;

    /**
     * 经办人id
     */
    private String operatorId;
}