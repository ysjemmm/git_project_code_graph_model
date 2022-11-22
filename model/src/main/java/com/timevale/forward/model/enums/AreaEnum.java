package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@AllArgsConstructor
public enum AreaEnum {
    /**
     *
     */
    SOUTH_CHINA(0,"华南大区" ),

    NORTH_CHINA(1,"华北大区"),

    EAST_CHINA(2,"华东大区"),

    WEST(3,"西部大区"),

    SHANG_HAI(4,"上海大区"),

    MIDDLE_CHINA(5,"华中大区"),

    OTHER(9,"其他大区");


    final private Integer code;
    final private String text;

    public static String getTextByCode(Integer code){
        for (AreaEnum e : AreaEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }

}
