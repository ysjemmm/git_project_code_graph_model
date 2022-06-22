package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

/**
 * @author jingchun
 * create on 2022/6/22
 */
@Getter
@AllArgsConstructor
public enum ProjectGoalTypeEnum {
    QUANTIFY(0, "定量"),
    QUALIFY(1, "定性")
    ;
    private final Integer code;
    private final String text;

    public String getTextByCode(Integer code) {
        for (ProjectGoalTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.text;
            }
        }
        return StringUtils.EMPTY;
    }

}
