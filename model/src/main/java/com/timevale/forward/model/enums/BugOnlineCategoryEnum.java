package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2023/03/22 17:23
 */
@Getter
@AllArgsConstructor
public enum BugOnlineCategoryEnum {
    FUNCTION(1, "功能问题", 15),
    PERFORMANCE(2, "性能问题", 10),
    CAPABILITY(3, "兼容性问题", 10),
    UE(4, "用户体验问题", 5),
    SECURITY(5, "安全问题", 15),
    ;

    private final Integer code;
    private final String text;
    private final Integer score;

    public static String getTextByCode(Integer code) {
        for (BugOnlineCategoryEnum e : BugOnlineCategoryEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }

    public static BugOnlineCategoryEnum getByCode(Integer code) {
        for (BugOnlineCategoryEnum value : BugOnlineCategoryEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
