package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 10:59
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDemandDO extends BaseDO {

    /**
     * 业务需求主题
     */
    String name;

    /**
     * 产品线id
     */
    Long productLineId;

    /**
     * 数据指标
     */
    String dataIndicators;

    /**
     * 目标客户/用户/项目
     */
    String targetCustomer;

    /**
     * 是否共创用户
     */
    Boolean createCustomer;

    /**
     * 部门id
     */
    Long deptId;

    /**
     * 需求描述
     */
    String desc;

    /**
     * 需求解决状态
     */
    Byte status;

    /**
     * 优先级
     */
    Byte priority;

    /**
     * 接收人
     */
    String receiveMan;

    /**
     * 接收人id
     */
    String receiveManId;

    /**
     * 处理器
     */
    Byte processor;

    /**
     * 操作系统
     */
    Byte os;

    /**
     * 计划发布日期
     */
    Integer planReleaseDate;

    /**
     * 驳回理由
     */
    Byte reason;
}
