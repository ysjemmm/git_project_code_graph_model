package com.timevale.forward.model.enums;

import com.timevale.mandarin.base.util.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum ProjectValidStageEnum {
    START("启动阶段"),

    PLAN("规划阶段"),

    EXECUTE("执行阶段"),

    FINISH("收尾阶段"),

    OPERATE("运营阶段");

    final private String text;

    /**
     * 得到所有阶段json
     *
     * @return {@link String}
     */
    public static String getAllStageJson() {
        ProjectValidStageEnum[] values = ProjectValidStageEnum.values();
        List<String> stages = Arrays.stream(values).map(ProjectValidStageEnum::getText).collect(Collectors.toList());
        return JsonUtils.obj2json(stages);
    }
}
