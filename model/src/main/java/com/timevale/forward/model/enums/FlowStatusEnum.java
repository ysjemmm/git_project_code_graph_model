package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @author jingchun
 * create on 2022/7/4
 */
@Getter
@AllArgsConstructor
public enum FlowStatusEnum {

    PRE_EDIT(-2, "提前编写文档"),
    WITHDRAW(-1, "已撤回"),
    AUDITING(0, "审核中"),
    COMPLETE(1, "审核通过"),
    REJECT(2, "审核不通过")
    ;
    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (FlowStatusEnum value : values()) {
            if (Objects.equals(value.code, code)) {
                return value.text;
            }
        }
        return StringUtils.EMPTY;
    }
}
