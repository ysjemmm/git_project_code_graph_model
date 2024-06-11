package com.timevale.forward.model.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;


/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@AllArgsConstructor
public enum ProjectAcceptanceStatusEnum {
    WITHDRAW(-1, "撤回"),
    DURING_ACCEPTANCE(0, "验收中"),
    ACCEPTANCE_PASSED(1, "验收通过"),
    ACCEPTANCE_NOT_PASSED(2, "验收不通过");

    final private Integer code;
    final private String text;

    private static final Map<Integer, ProjectAcceptanceStatusEnum> MAP =
            Maps.uniqueIndex(Arrays.asList(values()), ProjectAcceptanceStatusEnum::getCode);

    public static ProjectAcceptanceStatusEnum getByCode(Integer code) {
        return MAP.get(code);
    }

}
