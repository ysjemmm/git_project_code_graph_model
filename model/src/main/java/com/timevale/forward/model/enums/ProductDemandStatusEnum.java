package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProductDemandStatusEnum {
    /**
     * 0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废
     */
    WAITING(0,"待排期"),

    INCLUDED(10,"已列入项目"),

    PROGRESS(20,"项目进行中"),

    ONLINE(30,"已完成上线"),

    SUSPEND(40,"已暂停"),

    INVALID(50,"已作废");

    final private Integer code;

    final private String text;

    ProductDemandStatusEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }
    public static String getTextByCode(Integer code){
        for (ProductDemandStatusEnum e : ProductDemandStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
