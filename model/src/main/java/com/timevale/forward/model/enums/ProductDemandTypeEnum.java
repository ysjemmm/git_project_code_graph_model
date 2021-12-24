package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProductDemandTypeEnum {
    /**
     * 需求类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求(多选)
     */
    NEW_FEATURES(0,"新增功能"),

    RENEW_FEATURES(1,"功能迭代"),

    OPTIMIZE_PERFORMANCE(2,"体验优化"),

    TECH_DEMAND(3,"技术需求"),

    SECURITY_DEMAND(4,"安全需求");


    final private Integer code;

    final private String text;

    ProductDemandTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }
    public static String getTextByCode(Integer code){
        for (ProductDemandTypeEnum e : ProductDemandTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
