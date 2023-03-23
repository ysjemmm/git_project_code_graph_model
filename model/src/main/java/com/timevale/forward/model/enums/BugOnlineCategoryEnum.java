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

    NULL(0, "空"),
    FUNCTION(1, "功能问题"),
    PERFORMANCE(2, "性能问题"),
    CAPABILITY(3, "兼容性问题"),
    UE(4, "用户体验问题"),
    SECURITY(5, "安全问题"),
    ;

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (BugOnlineCategoryEnum e : BugOnlineCategoryEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
