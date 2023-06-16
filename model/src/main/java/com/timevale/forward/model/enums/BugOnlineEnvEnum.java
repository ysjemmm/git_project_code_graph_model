package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Date 2022/3/18 15:14
 * @Author 望轩
 */
@Getter
@AllArgsConstructor
public enum BugOnlineEnvEnum {
    /**
     * 生产环境
     */
    PRODUCE_ENV(0, "生产环境", 20),

    /**
     * 模拟环境
     */
    IMITATE_ENV(1, "模拟环境", 5);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static String getTextByCode(Integer code) {
        for (BugOnlineEnvEnum e : BugOnlineEnvEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }

    public static BugOnlineEnvEnum getByCode(Integer code) {
        for (BugOnlineEnvEnum e : BugOnlineEnvEnum.values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
