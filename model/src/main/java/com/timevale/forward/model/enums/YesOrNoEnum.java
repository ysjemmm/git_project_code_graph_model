package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@AllArgsConstructor
public enum YesOrNoEnum {
    /**
     *
     */
    NO(0,"否" ),

    YES(1,"是");


    final private Integer code;

    final private String text;


    YesOrNoEnum(Integer code, String text, String textChinese){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (YesOrNoEnum e : YesOrNoEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }

}
