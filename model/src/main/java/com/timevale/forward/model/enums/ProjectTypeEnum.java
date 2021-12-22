package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProjectTypeEnum {
    /**
     * 项目类型:0产品研发项目,1技术优化项目,2日常迭代
     */
    DEV(0,"产品研发项目"),

    OPTIMIZE(1,"技术优化项目"),

    RENEW(2,"日常迭代");

    final private Integer code;

    final private String text;

    ProjectTypeEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }
    public static String getTextByCode(Integer code){
        for (ProjectTypeEnum e : ProjectTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
