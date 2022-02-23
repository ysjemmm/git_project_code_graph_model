package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/23 17:05
 */
@Getter
@AllArgsConstructor
public enum BugEnvEnum {

    /**
     * 项目环境
     */
    PROJECT(0,"项目环境"),

    /**
     * 测试环境
     */
    TEST(1,"测试环境"),

    /**
     * 模拟环境
     */
    PREPARE(2,"模拟环境"),

    /**
     * 生产环境
     */
    production(3,"生产环境")

    ;

    private final Integer code;
    private final String text;
}
