package com.timevale.forward.dal.entity;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/23 10:12
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BizDemandListDO extends BaseDO {

    /**
     * 业务需求主题
     */
    private String name;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 目标客户
     */
    private String targetCustomer;

    /**
     * 业务域id
     */
    private String bizDomainId;

    /**
     * 业务域名称
     */
    private String bizDomainName;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 产品线名称
     */
    private String productLineName;

    /**
     * 预计上线时间
     */
    private Integer planReleaseDate;

    /**
     * 需求解决状态
     */
    private Integer status;

    /**
     * 接收人
     */
    private String receiveMan;

    /**
     * 接收人id
     */
    private String receiveManId;

    /**
     * 提交人
     */
    private String submitMan;

    /**
     * 提交人id
     */
    private String submitManId;

    /**
     * 部门id
     */
    private Long deptId;

    /**
     * 项目发布日期
     */
    private Date projectEndDate;

    /**
     * UED资源评估（人天）
     */
    private BigDecimal uedTime;

    /**
     * 后端资源评估（人天）
     */
    private BigDecimal backTime;

    /**
     * 前端资源评估（人天）
     */
    private BigDecimal frontTime;

    /**
     * 测试资源评估（人天）
     */
    private BigDecimal qaTime;

    /**
     * 总资源评估（人天）
     */
    private BigDecimal totalTime;

    /**
     * 需求描述
     */
    private String desc;

    /**
     * 期望上线时间
     */
    private Integer hopeReleaseDate;

    /**
     * 客户开发需求
     */
    private Boolean customerDevDemand;
}

