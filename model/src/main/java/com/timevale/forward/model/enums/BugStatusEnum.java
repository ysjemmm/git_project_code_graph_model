package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/23 16:59
 */
@Getter
@AllArgsConstructor
public enum BugStatusEnum {
    /**
     * bug打开
     */
    OPEN(0,"打开"),

    /**
     * 待修复
     */
    REPAIR(1,"待修复"),

    /**
     * 待验收
     */
    ACCEPTANCE(2, "待验收"),

    /**
     * 待确认
     */
    CONFIRM(3,"待确认"),

    /**
     * 延迟修复
     */
    POSTPONE_REPAIR(4,"延迟修复"),

    /**
     * 完成
     */
    COMPLETE(5,"完成"),

    /**
     * 关闭
     */
    CLOSE(6,"关闭");

    private final Integer code;
    private final String text;
}
