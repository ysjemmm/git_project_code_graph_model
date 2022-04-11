package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 商业变化日志做
 *
 * @author caibingxu
 * @date 2022/04/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BizChangeLogDO extends BaseDO{
    /**
     * 主体id
     */
    private Long mainId;

    /**
     * 内容变更记录类型:2项目,3产品需求,4业务需求
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
