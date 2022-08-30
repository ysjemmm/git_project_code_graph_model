package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by xingyun
 * @date 2021/12/15 17:13
 */
@Getter
public enum ProblemTypeEnum {
    /**
     * 问题类型 0功能缺失、1产品不可用、2界面不美观、3复杂难用、9其他
     */
    LACK_OF_FUNCTION(0, "功能缺失"),

    PRODUCT_UNAVAILABLE(1, "产品不可用"),

    UI_UNBEAUTY(2, "界面不美观"),

    DIFFICULT_TO_USE(3, "复杂难用"),

    OTHER(9, "其他");

    private final Integer code;
    private final String text;

    ProblemTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (ProblemTypeEnum e : ProblemTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }


}
