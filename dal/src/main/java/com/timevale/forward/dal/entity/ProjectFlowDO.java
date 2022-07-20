package com.timevale.forward.dal.entity;

import java.util.Date;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectFlowDO extends BaseDO {
    /**
     * 项目id
     */
    private Long projectId;


    /**
     * flowId
     */
    private String flowId;

    /**
     * 流程类型
     */
    private Integer flowType;

    /**
     * 发起人
     */
    private String proposer;

    /**
     * 发起人id
     */
    private String proposerId;


    /**
     * 会议时间
     */
    private Date reviewDate;

    /**
     * 评审人id
     */
    private String review;

    /**
     * 评审人
     */
    private String reviewId;

    /**
     * 评审人id
     */
    private String reviewed;

    /**
     * 评审人
     */
    private String reviewedId;

    /**
     * 未评审人
     */
    private String unreviewed;

    /**
     * 未评审人
     */
    private String unreviewedId;

    /**
     * 评审不通过人员
     */
    private String reviewFail;

    /**
     * 评审不通过人员
     */
    private String reviewFailId;

    /**
     * 未通过原因
     */
    private String reviewFailReason;

    /**
     * 状态
     */
    private Integer status;
    /**
     * 详设地址
     */
    private String reviewUrl;



}
