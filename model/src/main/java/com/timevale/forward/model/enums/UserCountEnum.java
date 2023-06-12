package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/06/09 17:40
 */
@Getter
@AllArgsConstructor
public enum UserCountEnum {

    LEQ_TWO(1, "1~2个", 0),
    GEQ_THI(2, "3个或3个以上", 10);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static UserCountEnum getByCode(Integer code) {
        for (UserCountEnum value : UserCountEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
