package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/25 15:21
 */
@Getter
@AllArgsConstructor
public enum BizChangeLogFieldEnum {
    /**
     * 项目
     */
    PROJECT_STATUS("项目状态"),

    PD("产品经理"),

    PLAN_END_DATE("项目计划结束时间"),

    PROJECT_GOAL("项目目标"),

    WITH_GOAL("是否有项目目标"),

    PJ_ESTABLISH_PUBLISH_DATE("立项预期上线时间"),

    PJ_ESTABLISH_START_DATE("立项开始时间"),

    /**
     * 项目目标
     */
    MAIN_GOAL("是否主目标"),

    GOAL_STATUS("完成状态"),

    GOAL_COMPLETE_NOTE("完成情况"),

    GOAL_REACH_VALUE("项目目标达标值"),

    /**
     * 产品需求
     */
    OWNER("需求负责人"),

    PRODUCT_DEMAND_STATUS("产品需求状态"),

    PRODUCT_DEMAND_TYPE("产品需求类型"),

    TRACK_EVENT("埋点事件"),

    /**
     * 业务需求
     */
    BIZ_DEMAND_STATUS("需求解决状态"),

    RECEIVE_MAN("需求接收人"),

    CREATE_MAN("需求提交人"),

    REASON("驳回理由"),

    REJECT_REASON("拒绝原因"),

    SOLVE_PLAN("处理方案"),

    PLAN_RELEASE_DATE("预期上线时间"),

    PROJECT_RELEASE_DATE("项目发布时间"),

    DEPARTMENT("需求部门"),

    /**
     * 其他
     */
    PRODUCT_LINE("产品线"),

    DESC("需求描述"),

    LABEL("标签"),

    ATTACHMENT("附件名称"),

    SUSPEND_REASON("项目暂停原因"),

    INVALID_REASON("项目作废原因"),
    ;

    private final String text;

}
