package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author yangxu
 * @date 2022/04/12
 */
@Getter
@AllArgsConstructor
public enum BizDemandPriorityEnum {

    P0(0, "紧急"),

    P1(10, "高"),

    P2(20, "中"),

    P3(30, "低");

    final private Integer code;
    final private String text;


    public static String getTextByCode(Integer code){
        for (BizDemandPriorityEnum e : BizDemandPriorityEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return StringUtils.EMPTY;
    }
}
