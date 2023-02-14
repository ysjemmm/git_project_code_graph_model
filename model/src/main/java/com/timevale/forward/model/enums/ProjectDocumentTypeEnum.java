package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

import static com.timevale.forward.model.enums.ProjectCategoryEnum.INNER_PROJECT;
import static com.timevale.forward.model.enums.ProjectCategoryEnum.PRODUCT_PROJECT;
import static com.timevale.forward.model.enums.ProjectStageEnum.*;

/**
 * @author jingchun
 * created on 2023/2/10
 */
@Getter
@AllArgsConstructor
public enum ProjectDocumentTypeEnum {

    NULL(0, "未知", ProjectCategoryEnum.NULL, ProjectStageEnum.NULL),
    PRODUCT(1, "产品需求文档", PRODUCT_PROJECT, ProjectStageEnum.NULL),
    SET_UP(11, "立项申请报告", INNER_PROJECT, START),
    SCHEME(12, "项目方案报告", INNER_PROJECT, PLAN),
    AUDIT(13, "审计计划", INNER_PROJECT, EXECUTE),
    REPLAY(14, "项目复盘报告", INNER_PROJECT, FINISH),
    OPERATION(15, "运营计划", INNER_PROJECT, OPERATE),
    OTHER(16, "其他", ProjectCategoryEnum.NULL, ProjectStageEnum.NULL),
    ;

    private final Integer code;
    private final String text;
    private final ProjectCategoryEnum category;
    private final ProjectStageEnum stage;

    public static ProjectDocumentTypeEnum getByCode(Integer code) {
        for (ProjectDocumentTypeEnum value : values()) {
            if (Objects.equals(value.code, code)) {
                return value;
            }
        }
        return NULL;
    }

    public static String getTextByCode(Integer code) {
        return getByCode(code).text;
    }

    public boolean isInner() {
        return category == INNER_PROJECT;
    }

}
