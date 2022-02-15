package com.timevale.forward.model.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @date 2022/1/24 15:13
 * @author 望轩
 */
@Getter
public enum ProjectNodeEnum {
    /**
     * 节点阶段
     */
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

    public final static Map<Integer, String> DEFAULT_NODE = new HashMap<Integer, String>() {{
        put(1, ProjectNodeEnum.START_PLAN.getProjectNodeName());
        put(2, ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getProjectNodeName());
        put(3, ProjectNodeEnum.DEMAND_CONSTRUE.getProjectNodeName());
        put(4, ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getProjectNodeName());
        put(5, ProjectNodeEnum.DEVELOP_START.getProjectNodeName());
        put(6, ProjectNodeEnum.WRITE_TEST_CASES.getProjectNodeName());
        put(7, ProjectNodeEnum.USE_CASE_REVIEW.getProjectNodeName());
        put(8, ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName());
        put(9, ProjectNodeEnum.TEST_START.getProjectNodeName());
        put(10, ProjectNodeEnum.PUBLISH_SIMULATE.getProjectNodeName());
        put(11, ProjectNodeEnum.PUBLISH_OFFICIAL.getProjectNodeName());
    }};
}
