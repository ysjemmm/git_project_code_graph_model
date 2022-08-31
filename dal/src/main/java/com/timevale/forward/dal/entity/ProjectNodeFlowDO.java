package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectNodeFlowDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     *流程id
     */
    private String flowId;

    /**
     *流程id
     */
    private String lastFlowId;

    /**
     * 阶段
     */
    private Integer stage;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 流程类型
     */
    private Integer flowType;
    /**
     *产品经理
     */
    private String pdId;

    /**
     *产品经理
     */
    private String pd;
    /**
     *业务方
     */
    private String bizId;

    /**
     *业务方
     */
    private String biz;
    /**
     *po负责人
     */
    private String poId;

    /**
     *po负责人
     */
    private String po;

    /**
     *D层负责人
     */
    private String did;

    /**
     *D层负责人
     */
    private String d;

    /**
     *延迟(工作日)
     */
    private BigDecimal delayDay;
    /**
     *未评审人员
     */
    private String unreviewedId;

    /**
     *未评审人员
     */
    private String unreviewed;

    /**
     *评审不通过人员
     */
    private String reviewFailId;

    /**
     *评审不通过人员
     */
    private String reviewFail;

    /**
     *未通过原因
     */
    private String reviewFailReason;
    /**
     *调整前发布时间
     */
    private Date publishDate;
    /**
     *调整后发布时间
     */
    private Date changePublishDate;

    /**
     * 立项预期上线时间
     */
    private Date pjEstablishPublishDate;

    /**
     *变更事由
     */
    private String reason;

    /**
     *变更类型
     */
    private Integer changeType;


    /**
     *变更类型为其他时,填写
     */
    private String otherReason;

    /**
     *变更类型为其他时,填写
     */
    private Date flowEndDate;


}
