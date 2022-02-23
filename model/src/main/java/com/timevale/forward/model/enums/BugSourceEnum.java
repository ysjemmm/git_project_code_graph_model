package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/23 17:59
 */
@Getter
@AllArgsConstructor
public enum BugSourceEnum {

    /**
     * 预演bug
     */
    PREVIEW(0,"预演bug"),

    /**
     * 测试阶段bug
     */
    TEST(1,"测试阶段bug"),

    /**
     * 历史版本bug
     */
    HISTORY(2,"历史版本bug"),

    /**
     * 自动化脚本执行发现bug
     */
    SCRIPT(3,"自动化脚本执行发现bug");

    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code){
        for (BugSourceEnum e : BugSourceEnum.values()){
            if(e.code.equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
