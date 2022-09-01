package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/08/19 17:23
 */
@Getter
@AllArgsConstructor
public enum AuditStatusEnum {
    APPROVE(0,"审核通过"),

    REJECT(10,"已驳回"),

    AUDITING(20,"审核中");

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code) {
        for (AuditStatusEnum e : AuditStatusEnum.values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "";
    }
}
