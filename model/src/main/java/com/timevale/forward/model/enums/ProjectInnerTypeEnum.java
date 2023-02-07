package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProjectInnerTypeEnum {

    //0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目

    NULL(0,"空"),

    STRATEGY(1,"战略项目"),

    LTC(2,"LTC项目"),

    PBG(3,"PBG项目"),

    CBG(4,"CBG项目"),

    MANAGE(5,"管理后台项目");

    final private Integer code;
    final private String text;

    public static String getTextByCode(Integer code){
        for (ProjectInnerTypeEnum e : ProjectInnerTypeEnum.values()) {
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
