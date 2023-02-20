package com.timevale.forward.model.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.timevale.forward.model.enums.ProjectCategoryEnum.INNER_PROJECT;
import static com.timevale.forward.model.enums.ProjectCategoryEnum.PRODUCT_PROJECT;

@Getter
@AllArgsConstructor
public enum ProjectStageEnum {

    NULL(-1, "未知", ProjectCategoryEnum.NULL, ProjectStatusEnum.NULL),
    DEMAND(0, "需求规划阶段", PRODUCT_PROJECT, ProjectStatusEnum.WAITING),
    DEV(1, "研发阶段", PRODUCT_PROJECT, ProjectStatusEnum.PLANING),
    TEST(2, "测试阶段", PRODUCT_PROJECT, ProjectStatusEnum.TESTING),
    START(11, "启动阶段", INNER_PROJECT, ProjectStatusEnum.WAITING),
    PLAN(12, "规划阶段", INNER_PROJECT, ProjectStatusEnum.PLANING),
    EXECUTE(13, "执行阶段", INNER_PROJECT, ProjectStatusEnum.EXECUTING),
    FINISH(14, "收尾阶段", INNER_PROJECT, ProjectStatusEnum.FINISHING),
    OPERATE(15, "运营阶段", INNER_PROJECT, ProjectStatusEnum.OPERATING);

    private final Integer code;
    private final String text;
    // 区分阶段属于内部项目还是产研项目
    private final ProjectCategoryEnum category;
    // 阶段所对应项目阶段
    private final ProjectStatusEnum status;

    // 由于用code查询比较平凡，缓存到一个map以提升性能
    private static final Map<Integer, ProjectStageEnum> MAP =
            Maps.uniqueIndex(Arrays.asList(values()), ProjectStageEnum::getCode);

    // 内部项目阶段集合
    private static final List<ProjectStageEnum> innerStageList = Arrays.stream(ProjectStageEnum.values())
            .filter(e -> ProjectCategoryEnum.INNER_PROJECT.equals(e.getCategory()))
            .collect(Collectors.toList());

    /**
     * 根据code查询text
     */
    public static String getTextByCode(Integer code) {
        return getByCode(code).text;
    }

    /**
     * 根据code查询项目阶段
     */
    public static ProjectStageEnum getByCode(Integer code) {
        return MAP.getOrDefault(code, NULL);
    }

    public static ProjectStageEnum getPreStage(Integer code) {
        if (code == null) {
            return null;
        }
        int index = innerStageList.size() - 1;
        for (int i = 0; i < innerStageList.size(); i++) {
            ProjectStageEnum stageEnum = innerStageList.get(i);
            if (stageEnum.code.equals(code)) {
                index = i - 1;
                break;
            }
        }
        return index < 0 ? null : innerStageList.get(index);
    }

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
