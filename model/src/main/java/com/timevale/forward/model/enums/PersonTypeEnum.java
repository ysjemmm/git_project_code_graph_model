package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 09:50
 */
@Getter
public enum PersonTypeEnum {
    // 0 项目-产品经理
    PROJECT_PD(0),
    // 1 项目-项目成员
    PROJECT_MEMBER(1),
    // 20 产品需求-抄送人
    PRODUCT_DEMAND_CC(20),
    // 30 业务需求-抄送人
    BIZ_DEMAND_CC(30),
    // 40 任务执行人
    TASK_EXECUTOR(40),
    // 50 线下bug-抄送人
    BUG_OFFLINE_CC(50)
    ;

    private Integer code;

    PersonTypeEnum(Integer code){this.code = code;}

}
