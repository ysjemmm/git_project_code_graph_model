package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
@AllArgsConstructor
public enum ProjectStatusEnum {
    /**
     * 0待启动,10规划中,15执行中,20研发中,25收尾中,30测试中,35运营中,40已发布,45已完成,-10已暂停,-20已作废
     */
    WAITING(0,"待启动"),

    PLANING(10,"规划中"),

    DEVING(20,"研发中"),

    FINISHING(25,"收尾中"),

    TESTING(30,"测试中"),

    OPERATING(35,"运营中"),

    RELEASED(40,"已发布"),

    COMPLETE(45,"已完成"),

    SUSPEND(-10,"已暂停"),

    INVALID(-20,"已作废");

    final private Integer code;
    final private String text;

    public static String getTextByCode(Integer code){
        for (ProjectStatusEnum e : ProjectStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }

    public static Boolean ongoing(Integer code){
        return code.equals(PLANING.code) || code.equals(DEVING.code) || code.equals(TESTING.code);
    }

    public static Boolean terminated(Integer code){
        return code.equals(RELEASED.code) || code.equals(SUSPEND.code) || code.equals(INVALID.code);
    }

    public static Boolean canNotUpdate(Integer code){
        return code.equals(RELEASED.code) || code.equals(INVALID.code);
    }

}
