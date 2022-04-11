package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BizDemandFieldEnum {
    NAME("业务需求主题"),
    PRODUCT_LINE("产品线"),
    DATA_INDICATORS("数据指标"),
    TARGET_CUSTOMER("目标客户"),
    CREATE_CUSTOMER("共创用户"),
    DEPARTMENT("部门"),
    DESCRIBE("描述"),
    STATUS("状态"),
    PRIORITY("优先级"),
    RECEIVE_MAN("接收人"),
    PLAN_RELEASE_DATE("计划发布日期"),
    REASON("驳回原因"),
    ;

    private final String text;
}
