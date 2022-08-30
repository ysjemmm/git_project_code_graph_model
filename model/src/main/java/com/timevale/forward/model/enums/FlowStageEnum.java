package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 @Getter
 */
@Getter
public enum FlowStageEnum {
    /**
     * 项目流程阶段
     */
    FIRST(0, "一阶段"),

    SECOND(1, "二阶段");

    private final Integer code;
    private final String text;

    FlowStageEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (FlowStageEnum e : FlowStageEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
