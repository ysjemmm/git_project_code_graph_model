package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 15:14
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlineEnvStatus {
    /**
     * 生产环境
     */
    PRODUCE_ENV(0, "生产环境"),

    /**
     * 模拟环境
     */
    IMITATE_ENV(1, "模拟环境");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineEnvStatus e : BugOnlineEnvStatus.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "error code";
    }
}
