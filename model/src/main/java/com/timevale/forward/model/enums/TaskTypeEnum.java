package com.timevale.forward.model.enums;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum TaskTypeEnum {

    NULL(-1, "其他", ProjectStageEnum.NULL),
    SURVEY(0, "调研", ProjectStageEnum.DEMAND),
    DETAIL_DESIGN(1, "详细设计", ProjectStageEnum.DEV),
    CASE_DESIGN(2, "测试用例设计", ProjectStageEnum.DEV),
    DEVELOP(3, "开发", ProjectStageEnum.DEV),
    INTEGRATED(4, "集测开发", ProjectStageEnum.DEV),
    CODE_REVIEW(5, "code review", ProjectStageEnum.DEV),
    TEST(6, "测试", ProjectStageEnum.TEST),
    OFFLINE_BUG(7, "线下bug修复", ProjectStageEnum.TEST),
    PUBLISH(8, "发布", ProjectStageEnum.TEST),
    ONLINE_BUG(9, "线上bug修复", ProjectStageEnum.TEST);

    private final Integer code;
    private final String text;
    // 区分类型属于产研项目哪个阶段
    private final ProjectStageEnum stage;

    // 由于用code查询比较频繁，缓存到一个map以提升性能
    private static final Map<Integer, TaskTypeEnum> MAP =
            Maps.uniqueIndex(Arrays.asList(values()), TaskTypeEnum::getCode);

    /**
     * 根据code查询text
     */
    public static String getTextByCode(Integer code) {
        return getByCode(code).text;
    }

    /**
     * 根据code查询任务类型
     */
    public static TaskTypeEnum getByCode(Integer code) {
        return MAP.getOrDefault(code, NULL);
    }

    /**
     * 得到阶段Code
     *
     * @return {@link String}
     */
    public static Integer getStageCode(Integer type) {
        return getByCode(type).stage.getCode();
    }
}
