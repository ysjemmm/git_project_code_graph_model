package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum TaskStageEnum {
    /**
     * 阶段:0需求规划阶段,1研发阶段,2测试阶段
     */
    DEMAND(0,"需求规划阶段"),

    DEV(1,"研发阶段"),

    TEST(2,"测试阶段");


    final private Integer code;

    final private String text;

    TaskStageEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }
    public static String getTextByCode(Integer code){
        for (TaskStageEnum e : TaskStageEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
