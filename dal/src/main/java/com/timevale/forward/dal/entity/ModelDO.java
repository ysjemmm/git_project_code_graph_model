package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ModelDO extends BaseDO {

    /**
     * 业务域id
     */
    private Long productLineId;

    /**
     * 产品线名称
     */
    private String name;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人id
     */
    private String ownerId;

    /**
     * 表单字段
     */
    private String formField;
}
