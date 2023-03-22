package com.timevale.forward.model.enums;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 */
@Getter
public enum MessageTagEnum {
    /**
     * 项目流程状态
     */
    FORWARD_TECHREVIEW("forward_techReview"),

    FORWARD_TRACKEVENTREVIEW("forward_trackEventReview"),

    FORWARD_TRACKEVENTREVIEW_NOTNOTICE("forward_trackEventReview_notNotice"),

    FORWARD_PRODUCT_DEMAND_CHANGE("forward_productDemandChange"),

    FORWARD_PUBLISHOFFICEREVIEW("forward_publishOfficeReview"),

    /**
     * 发起需求内审流程
     */
    FORWARD_DEMAND_INTERNAL_AUDIT("forward_demandInternalAudit"),

    /**
     * 发起需求串讲流程
     */
    FORWARD_DEMAND_CONSTRUE("forward_demandConstrue"),

    /**
     * 发起ued评审流程
     */
    FORWARD_UED_AUDIT("forward_uedAudit"),

    /**
     * 发起结项流程
     */
    FORWARD_PROJECT_CONCLUSION("forward_project_conclusion"),

    /**
     * 工作量变更申请
     */
    FORWARD_WORKLOAD_CHANGE("forward_workload_change")
    ;

    private final String text;

    public final static Map<Integer, String> NODE_MESSAGE_TAG_MAP = new LinkedHashMap<>();


    static {

        NODE_MESSAGE_TAG_MAP.put(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getCode(),MessageTagEnum.FORWARD_DEMAND_INTERNAL_AUDIT.getText());
        NODE_MESSAGE_TAG_MAP.put(ProjectNodeEnum.DEMAND_CONSTRUE.getCode(),MessageTagEnum.FORWARD_DEMAND_CONSTRUE.getText());
        NODE_MESSAGE_TAG_MAP.put(ProjectNodeEnum.UED_AUDIT.getCode(),MessageTagEnum.FORWARD_UED_AUDIT.getText());
        NODE_MESSAGE_TAG_MAP.put(ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getCode(),MessageTagEnum.FORWARD_TECHREVIEW.getText());

    }

    MessageTagEnum(String text){
        this.text = text;
    }

}
