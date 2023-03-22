package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2023/03/06 10:49
 */
@Getter
@AllArgsConstructor
public enum DelayTypeEnum {
    /**
     * 不延期
     */
    NONE(-1),

    /**
     * 提测延期
     */
    SUBMIT_TEST_DELAY(0),

    /**
     * 发布正式延期
     */
    PUBLISH_DELAY(1),

    /**
     * 立项预期上线时间小于发布正式计划时间
     */
    ESTIMATED_DELAY(2);

    private final Integer code;
}
