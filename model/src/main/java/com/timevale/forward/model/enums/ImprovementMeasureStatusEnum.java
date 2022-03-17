package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/03/17 11:49
 */
@Getter
@AllArgsConstructor
public enum ImprovementMeasureStatusEnum {

    /**
     * 待处理
     */
    PENDING(0,"待处理"),

    /**
     * 已完成
     */
    COMPLETED(1,"已完成");

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code){
        for (ImprovementMeasureStatusEnum e : ImprovementMeasureStatusEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
