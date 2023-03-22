package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;

/**
 * @author by YangXu
 * @date 2023/03/03 18:24
 */
@Getter
@AllArgsConstructor
public enum ProjectKindEnum {

    NULL(0,""),

    PBG_BASE(1,"PBG项目/基线项目"),

    PBG_OTN(2,"PBG项目/1-N客开项目"),

    OFC_FLOW_IT(3,"职能后台项目/流程IT中心项目");

    private final Integer code;
    private final String text;

    public static ProjectKindEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        ProjectKindEnum[] values = ProjectKindEnum.values();
        for (ProjectKindEnum e : values) {
            if (Objects.equals(e.code, code)) {
                return e;
            }
        }
        return null;
    }

    public static String getTextByCode(Integer code){
        for (ProjectKindEnum e : ProjectKindEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
