package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;


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

    public static String getTextByCode(Integer code){
        for (YesOrNoEnum e : YesOrNoEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }

    public static String getTextByCode(Boolean code){
        return BooleanUtils.isTrue(code) ? YES.text : NO.text;
    }

}
