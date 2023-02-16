package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/21 18:17
 */
@Getter
@AllArgsConstructor
public enum CommentTypeEnum {
    // 主体类型:
    // 0项目
    PROJECT(0,"项目"),
    // 1产品需求
    PRODUCT_DEMAND(1,"产品需求"),
    // 2业务需求
    BIZ_DEMAND(2,"业务需求"),
    // 3任务
    TASK(3,"任务"),
    // 4线下bug
    BUG_OFFLINE(4,"线下bug"),
    // 5线上bug
    BUG_ONLINE(5,"线上bug"),
    // 6故障单
    TROUBLE_TICKET(6,"故障单"),
    // 7 客户需求
    CUSTOM_DEMAND(7,"客户需求"),
    // 8 内部项目
    INNER_PROJECT(8,"内部项目"),
    // 9 内部任务
    INNER_TASK(9, "内部任务"),
    ;

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (CommentTypeEnum e : CommentTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
