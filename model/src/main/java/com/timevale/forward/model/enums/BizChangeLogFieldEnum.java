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

    /**
     * 产品需求
     */
    OWNER("需求负责人"),

    PRODUCT_DEMAND_STATUS("产品需求状态"),

    PRODUCT_DEMAND_TYPE("产品需求类型"),

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

    DESC("需求描述");

    private final String text;

}
