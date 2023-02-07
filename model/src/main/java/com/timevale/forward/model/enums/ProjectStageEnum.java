package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.timevale.forward.model.enums.ProjectCategoryEnum.INNER_PROJECT;
import static com.timevale.forward.model.enums.ProjectCategoryEnum.PRODUCT_PROJECT;

@Getter
@AllArgsConstructor
public enum ProjectStageEnum {

    DEMAND(0, "需求规划阶段", PRODUCT_PROJECT),
    DEV(1, "研发阶段", PRODUCT_PROJECT),
    TEST(2, "测试阶段", PRODUCT_PROJECT),
    START(11, "启动阶段", INNER_PROJECT),
    PLAN(12, "规划阶段", INNER_PROJECT),
    EXECUTE(13, "执行阶段", INNER_PROJECT),
    FINISH(14, "收尾阶段", INNER_PROJECT),
    OPERATE(15, "运营阶段", INNER_PROJECT);

    private final Integer code;
    private final String text;
    // 区分阶段属于内部项目还是产研项目
    private final ProjectCategoryEnum category;

    /**
     * 得到所有阶段json
     *
     * @return {@link String}
     */
    public static String getAllStageJson(ProjectCategoryEnum category) {
        return "[" +
                Arrays.stream(values())
                        .filter(stage -> stage.getCategory().equals(category))
                        .map(ProjectStageEnum::getCode)
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) +
                "]";
    }
}
