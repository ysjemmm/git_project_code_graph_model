package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 产品需求分组表DO
 * 对应表：product_demand_group
 * @author by qiyuan
 * @date 2025/07/14 14:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ProductDemandGroupDO extends BaseDO {
    /**
     * 名称
     */
    private String name;

    /**
     * 业务域id
     */
    private Long bizDomainId;

    /**
     * 相对位置
     */
    private Double position;

    /**
     * 版本号
     */
    private Long version;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人id
     */
    private String ownerId;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 项目名
     */
    private Long projectName;

    /**
     * 项目状态：0-待启动，5-启动中，10-规划中，15-执行中，20-研发中，25-收尾中，30-测试中，35-运营中，40-已发布，45-已完成，50-已结项，-10-已暂停，-20-已作废
     */
    private Integer status;

    /**
     * 虚拟列，用于唯一索引
     * 已删除的这个字段为null
     */
    private Boolean isActive;
}