package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum PriorityEnum {
    /**
     * 优先级:0(P0),1(P1),2(P2)
     */
    P0(0,"P0"),

    P1(10,"P1"),

    P2(20,"P2"),

    P3(30,"P2");

    final private Integer code;

    final private String text;

    PriorityEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }
    public static String getTextByCode(Integer code){
        for (PriorityEnum e : PriorityEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
