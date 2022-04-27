package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/04/26 19:54
 */
@Getter
@AllArgsConstructor
public enum ProjectNodeStatusEnum {
    // 待启动、待内审、待串讲、待详设内审、待开发、开发中、待测试、测试中、已发布
    READY_START(0,"待启动"),

    READY_INTERNAL_AUDIT(10,"待内审"),

    READY_CONSTRUE(20,"待串讲"),

    READY_TECHNICAL_DETAIL_REVIEW(30,"待详设内审"),

    READY_DEVELOP(40,"待开发"),

    DEVELOPING(50,"开发中"),

    READY_TEST(60,"待测试"),

    TESTING(70,"测试中"),

    PUBLISHED(80,"已发布");

    private Integer code;
    private String text;

}
