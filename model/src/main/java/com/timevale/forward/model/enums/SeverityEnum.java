package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
public enum SeverityEnum {
    /**
     * 严重程度
     */
    P0(0,"阻塞"),

    P1(10,"严重"),

    P2(20,"一般"),

    P3(30,"轻微");

    final private Integer code;

    final private String text;

    SeverityEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (SeverityEnum e : SeverityEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
