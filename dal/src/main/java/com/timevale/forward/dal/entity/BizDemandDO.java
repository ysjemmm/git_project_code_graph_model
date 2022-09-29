package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

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
    private String name;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 数据指标
     */
    private String dataIndicators;

    /**
     * 目标客户/用户/项目
     */
    private String targetCustomer;

    /**
     * 是否共创用户
     */
    private Boolean createCustomer;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 需求描述
     */
    private String desc;

    /**
     * 需求解决状态
     */
    private Integer status;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 接收人
     */
    private String receiveMan;

    /**
     * 接收人id
     */
    private String receiveManId;

    /**
     * 计划发布日期
     */
    private Integer planReleaseDate;

    /**
     * 驳回理由
     */
    private Integer reason;

    /**
     * 需求提交人
     */
    private String submitMan;

    /**
     * 需求提交人id
     */
    private String submitManId;

    /**
     * 解决方案
     */
    private String solvePlan;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 项目发布时间
     */
    private Date projectEndDate;

    /**
     * 是否客开需求
     */
    private Boolean customerDevDemand;

    /**
     * 客开类型
     */
    private Integer customerDevType;

    /**
     * 销售合同编号
     */
    private String customerDevSaleContract ;

    /**
     * 项目名称
     */
    private String customerDevProjectName;

    /**
     * 来源id
     */
    private String sourceId;

    /**
     * 业务id
     */
    private String bizId;

    /**
     * 业务展示名称
     */
    private String bizName;
}
