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
public enum ForwardFlowStatusEnum {

    PRE_EDIT(-2, "提前编写文档", ""),
    WITHDRAW(-1, "已撤回", "REJECT"),
    AUDITING(0, "审核中","PENDING"),
    COMPLETE(1, "审核通过", "FLOW_COMPLETE"),
    REJECT(2, "审核不通过", "WITHDRAW")
    ;
    private final Integer code;
    private final String text;
    private final String value;

    public static String getTextByCode(Integer code) {
        for (ForwardFlowStatusEnum value : values()) {
            if (Objects.equals(value.code, code)) {
                return value.text;
            }
        }
        return StringUtils.EMPTY;
    }

    public static ForwardFlowStatusEnum getByValue(String value) {
        for (ForwardFlowStatusEnum element : values()) {
            if (Objects.equals(element.value, value)) {
                return element;
            }
        }
        return null;
    }
}
