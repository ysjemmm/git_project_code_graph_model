package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/06/09 17:40
 */
@Getter
@AllArgsConstructor
public enum ProblemOccurredTimeEnum {
    WITHIN_A_DAY(0, "24小时以内"),
    WITHIN_THREE_DAYS(0, "24~72小时"),
    MORE_THAN_THREE_DAYS(0, "72小时以上");

    private final Integer code;
    private final String text;
}
