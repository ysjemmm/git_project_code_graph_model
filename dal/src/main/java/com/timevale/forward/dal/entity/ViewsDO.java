package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户视图表
 * 对应表：views
 * @author by qiyuan
 * @date 2025/08/14 14:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ViewsDO extends BaseDO {
    /**
     * 名称
     */
    private String name;

    /**
     * 业务类型：10-业务需求，11-产品需求，12-项目，13-线下bug，14-线上bug
     */
    private Integer type;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人id
     */
    private String ownerId;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 筛选条件
     */
    private String filterCondition;

}