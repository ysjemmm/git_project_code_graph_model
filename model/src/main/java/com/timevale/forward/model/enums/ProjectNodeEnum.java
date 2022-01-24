package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @Date 2022/1/24 15:13
 * @Author 望轩
 */
@Getter
public enum ProjectNodeEnum {

    START_PLAN("开始规划"),
    DEMAND_INTERNAL_AUDIT("需求内审"),
    DEMAND_CONSTRUE("需求串讲"),
    TECHNICAL_DETAIL_REVIEW("技术详设评审"),
    DEVELOP_START("开发开始"),
    WRITE_TEST_CASES("编写测试用例"),
    USE_CASE_REVIEW("用例评审"),
    SUBMIT_TEST("提测"),
    TEST_START("测试开始"),
    PUBLISH_SIMULATE("发布模拟"),
    PUBLISH_OFFICIAL("发布正式");

    private final String projectNodeName;

    ProjectNodeEnum(String projectNodeName) {
        this.projectNodeName = projectNodeName;
    }
}
