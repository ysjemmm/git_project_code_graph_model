package com.timevale.forward.model.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;


/**
 * @author by YangXu
 * @date 2022/04/27 15:36
 */
@Getter
@AllArgsConstructor
public enum ProjectNodeEnum {
    /**
     * 节点阶段
     */
    START_PLAN(0, "开始规划"),
    DEMAND_INTERNAL_AUDIT(10, "需求内审"),
    DEMAND_CONSTRUE(20, "需求串讲"),
    DEMAND_CONSTRUE_REVERSE(25, "需求反串讲"),
    UED_AUDIT(27, "UED评审"),
    TECHNICAL_DETAIL_REVIEW(30, "技术详设评审"),
    DEVELOP_START(40, "开发开始"),
    WRITE_TEST_CASES(50, "编写测试用例"),
    USE_CASE_REVIEW(60, "用例评审"),
    SUBMIT_TEST(70, "提测"),
    TEST_START(80, "测试开始"),
    PUBLISH_SIMULATE(90, "发布模拟"),
    PUBLISH_OFFICIAL(100, "发布正式");

    private final Integer code;
    private final String text;

    private static final Map<Integer, ProjectNodeEnum> MAP =
            Maps.uniqueIndex(Arrays.asList(values()), ProjectNodeEnum::getCode);

    public static Integer getCodeByName(String name) {
        for (ProjectNodeEnum e : ProjectNodeEnum.values()) {
            if (Objects.equals(e.getText(), name)) {
                return e.code;
            }
        }
        return -1;
    }

    /**
     * 根据code获取对应的名称
     */
    public static ProjectNodeEnum getByCode(Integer code) {
        return MAP.get(code);
    }

    /**
     * 根据code获取对应的名称
     */
    public static String getNameByCode(Integer code) {
        return Optional.ofNullable(getByCode(code)).map(ProjectNodeEnum::getText).orElse(null);
    }

    public static boolean canStartFlow(String name) {
        return DEMAND_INTERNAL_AUDIT.getText().equals(name) ||
                DEMAND_CONSTRUE.getText().equals(name) ||
                UED_AUDIT.getText().equals(name) ||
                TECHNICAL_DETAIL_REVIEW.getText().equals(name);
    }
}
