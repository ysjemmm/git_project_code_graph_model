package com.timevale.forward.model.enums;

import lombok.Getter;
/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
public enum ProjectStageEnum {
    /**
     * 项目阶段
     */
    DEMAND_START("开始规划"),

    DEMAND_CHECK("需求内审"),

    DEMAND_ANALYSE("需求串讲"),

    DEV_REVIEW("技术详设评审"),
    
    DEV_START("开发开始"),
    
    TEST_START("测试开始"),
    
    TEST_RELEASE("发布正式");

    private final String text;
    ProjectStageEnum(String text){
        this.text = text;
    }
}
