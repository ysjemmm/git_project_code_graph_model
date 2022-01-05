package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/01/05 10:37
 */
@Getter
public enum ProductLineTypeEnum {
    // 产品线类型 0：默认产品线  1：金格产品线
    DEFAULT(0, "默认产品线"),
    KINGGRID(1, "金格产品线");

    private final Integer code;
    private final String text;

    ProductLineTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (ProductLineTypeEnum e : ProductLineTypeEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
