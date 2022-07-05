package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @author by YangXu
 * @date 2022/02/24 09:51
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class BizChangeLogDO extends BaseDO {
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

    /**
     * json 文本
     */
    private String content;

}
