package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/21 18:17
 */
@Getter
public enum CommentTypeEnum {
    // 主体类型: 0项目，1产品需求2业务需求
    PROJECT(0,"项目"),
    PRODUCT_DEMAND(1,"产品需求"),
    BIZ_DEMAND(2,"业务需求");

    private Byte code;
    private String text;

    CommentTypeEnum(Integer code, String text){
        this.code = code.byteValue();
        this.text = text;
    }

    public static String getTextByCode(Byte code) {
        for (CommentTypeEnum e : CommentTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.getText();
            }
        }
        return "errorCode";
    }
}
