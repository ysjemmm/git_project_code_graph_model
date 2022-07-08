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
public enum ProjectGoalStatusEnum {
    IN_PROGRESS(0, "进行中"),
    FINISHED(10, "已完成"),
    UNFINISHED(30, "未完成")
    ;
    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code) {
        for (ProjectGoalStatusEnum value : values()) {
            if (value.getCode().equals(code)) {
                return value.text;
            }
        }
        return StringUtils.EMPTY;
    }
}
