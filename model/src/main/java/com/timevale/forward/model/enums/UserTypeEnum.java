package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/01/24 15:13
 */
@Getter
public enum UserTypeEnum {
    /**
     * 产品
     */
    PD("产品"),

    /**
     * 开发
     */
    RD("开发"),

    /**
     * 测试
     */
    QA("测试");

    private final String jobFunction;

    UserTypeEnum(String jobFunction) {
        this.jobFunction = jobFunction;
    }
}
