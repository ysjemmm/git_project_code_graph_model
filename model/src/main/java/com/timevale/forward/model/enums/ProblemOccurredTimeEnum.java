package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/06/09 17:40
 */
@Getter
@AllArgsConstructor
public enum ProblemOccurredTimeEnum {
    WITHIN_A_DAY(0, "24小时以内", 0),
    WITHIN_THREE_DAYS(0, "24~72小时", 5),
    MORE_THAN_THREE_DAYS(0, "72小时以上", 10);

    private final Integer code;
    private final String text;
    private final Integer score;

    public static ProblemOccurredTimeEnum getByCode(Integer code) {
        for (ProblemOccurredTimeEnum value : ProblemOccurredTimeEnum.values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    public static String getTextByCode(Integer code) {
        return Optional.ofNullable(getByCode(code))
                .map(ProblemOccurredTimeEnum::getText)
                .orElse("");
    }
}
