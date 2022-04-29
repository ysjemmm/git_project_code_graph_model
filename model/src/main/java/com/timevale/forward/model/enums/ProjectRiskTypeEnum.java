package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/04/24 17:23
 */
@Getter
@AllArgsConstructor
public enum ProjectRiskTypeEnum {

    OTHER(0,"其它"),

    SUBMIT_FAILURE(10, "提测质量不达标"),

    TASK_OVERDUE(20, "任务逾期"),

    NODE_OVERDUE(30, "项目关键节点逾期"),

    NODE_ENTRY_OVERDUE(40, "项目节点逾期未录入");

    private Integer code;
    private String text;

    public static String getTextByCode(Integer code){
        for (ProjectRiskTypeEnum e : ProjectRiskTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }

    public static Integer getCodeByText(String text){
        for (ProjectRiskTypeEnum e : ProjectRiskTypeEnum.values()){
            if(e.getText().equals(text)){
                return e.code;
            }
        }
        return -1;
    }
}
