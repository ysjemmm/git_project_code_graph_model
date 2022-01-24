package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProjectStatusEnum {
    /**
     * 0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废
     */
    WAITING(0,"待启动"),

    PLANING(10,"规划中"),

    DEVING(20,"研发中"),

    TESTING(30,"测试中"),

    RELEASED(40,"已发布"),

    SUSPEND(-10,"已暂停"),

    INVALID(-20,"已作废");

    final private Integer code;

    final private String text;

    ProjectStatusEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (ProjectStatusEnum e : ProjectStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }

    public static Boolean ongoing(Integer code){
        return code.equals(PLANING.code) || code.equals(DEVING.code) || code.equals(TESTING.code);
    }

}
