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
    // 主体类型: 0项目，1产品需求2业务需求
    PROJECT(0,"项目"),
    PRODUCT_DEMAND(1,"产品需求"),
    BIZ_DEMAND(2,"业务需求"),
    TASK(3,"任务"),
    BUG_OFFLINE(4,"线下bug");

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
