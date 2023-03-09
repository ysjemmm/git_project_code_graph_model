package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2023/03/09 15:31
 */
@Getter
@AllArgsConstructor
public enum FlowTypeEnum {
    /**
     * 需求内审
     */
    DEMAND_INTERNAL_AUDIT(10, "需求内审"),
    /**
     * 需求串讲
     */
    DEMAND_CONSTRUE(20, "需求串讲"),
    /**
     * UED评审
     */
    UED_AUDIT(27, "UED评审"),
    /**
     * 技术详设评审
     */
    TECHNICAL_DETAIL_REVIEW(30, "技术详设评审"),
    /**
     * 项目结项流程
     */
    CONCLUSION(105,"项目结项流程"),
    /**
     * 工作量变更流程
     */
    WORKLOAD(106,"工作量变更流程")
    ;


    private final Integer code;
    private final String text;

    public static String getTextByCode(Integer code){
        for (FlowTypeEnum e : FlowTypeEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }
}
