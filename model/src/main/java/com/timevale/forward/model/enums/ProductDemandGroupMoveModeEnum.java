package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author qiyuan
 * create on 2025/07/22 15:00
 */
@Getter
@AllArgsConstructor
public enum ProductDemandGroupMoveModeEnum {
    FOLLOW("follow", "分组之间拖动"),
    MOVE_IN("moveIn", "待规划需求拖进来"),
    MOVE_OUT("moveOut", "需求拖到待规划列表");
    private final String code;
    private final String text;

    public static String getTextByCode(String code) {
        for (ProductDemandGroupMoveModeEnum e : values()) {
            if (e.code.equals(code)) {
                return e.text;
            }
        }
        return "errorCode";
    }

}
