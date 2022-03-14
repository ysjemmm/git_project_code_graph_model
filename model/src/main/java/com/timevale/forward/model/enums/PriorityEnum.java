package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
public enum PriorityEnum {
    /**
     * 优先级:0(P0),1(P1),2(P2)
     */
    P0(0,"P0", "紧急"),

    P1(10,"P1", "高"),

    P2(20,"P2", "中"),

    P3(30,"P3", "低");

    final private Integer code;

    final private String text;

    final private String textChinese;

    PriorityEnum(Integer code, String text, String textChinese){
        this.code = code;
        this.text = text;
        this.textChinese = textChinese;
    }

    public static String getTextByCode(Integer code){
        for (PriorityEnum e : PriorityEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }

    public static String getTextChineseByCode(Integer code){
        for (PriorityEnum e : PriorityEnum.values()){
            if(e.getCode().equals(code)){
                return e.textChinese;
            }
        }
        return "errorCode";
    }
}
