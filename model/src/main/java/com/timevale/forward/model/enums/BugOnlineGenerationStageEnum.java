package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;


@Getter
@AllArgsConstructor
public enum BugOnlineGenerationStageEnum {
    FIRST_DEPLOY_TEST(10,  "首次部署（测试阶段）"),
    DOCKING_TEST(20, "对接联调（测试环境）"),
    FIRST_DEPLOY_PROD(30, "首次部署（上线阶段）"),
    DAILY_USE_TEST(40, "日常使用（试运行）"),
    DAILY_USE(50, "日常使用"),
    DOCKING_PROD(60, "对接联调（正式环境）"),
    TEST_ENV_CHANGE(70, "测试环境变更"),
    PROD_ENV_CHANGE(80, "正式环境变更"),
    CONSULTING_QUESTION(90, "咨询类问题");

    private final Integer code;
    private final String text;

    public String getTextByCode(Integer code) {
        return Optional.ofNullable(getByCode(code))
                .map(BugOnlineGenerationStageEnum::getText)
                .orElse("");
    }

    public BugOnlineGenerationStageEnum getByCode(Integer code) {
        for (BugOnlineGenerationStageEnum value : BugOnlineGenerationStageEnum.values()) {
            if (value.getCode().equals(code)) {
                return value;
            }
        }
        return null;
    }
}
