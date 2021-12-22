package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProjectPriorityEnum {
    /**
     * 优先级:0(P0),1(P1),2(P2)
     */
    P0(0,"P0"),

    P1(1,"P1"),

    P2(2,"P2");

    final private Byte code;

    final private String text;

    ProjectPriorityEnum(Integer code, String text){
        this.code = code.byteValue();
        this.text = text;
    }
    public static String getTextByCode(Byte code){
        for (ProjectPriorityEnum e : ProjectPriorityEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
