package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author jingchun
 * created on 2022/9/8
 */
@Getter
@AllArgsConstructor
public enum LabelCategoryProtectionEnum {
    NONE(0, "不保护"),
    WD(1, "不可编辑和删除")
    ;
    private final Integer code;
    private final String text;


}
