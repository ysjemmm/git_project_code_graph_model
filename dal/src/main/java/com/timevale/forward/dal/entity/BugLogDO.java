package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author by YangXu
 * @date 2022/02/24 09:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BugLogDO extends BaseDO implements Serializable {

    /**
     * 主体id
     */
    private Long mainId;

    /**
     * 内容变更记录类型:0线下bug,1线上bug
     */
    private Integer type;

    /**
     * 变更前的值
     */
    private String oldValue;

    /**
     * 变更后的值
     */
    private String newValue;

    /**
     * 变更字段
     */
    private String field;

    /**
     * 按钮动作
     */
    private String action;

    /**
     * 单据名称
     */
    private String bugName;

    /**
     * 变更内容
     */
    private String content;
}
