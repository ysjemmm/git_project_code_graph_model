package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/02/24 09:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LogDO extends BaseDO {

    /**
     * 主体id
     */
    private Long mainId;

    /**
     * 内容变更记录类型:线下bug,线上bug,项目,产品需求,业务需求等
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
}
