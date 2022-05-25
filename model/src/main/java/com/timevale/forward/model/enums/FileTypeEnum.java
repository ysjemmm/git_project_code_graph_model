package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 10:18
 */
@Getter
@AllArgsConstructor
public enum FileTypeEnum {
    // 产品需求
    PRODUCT_DEMAND(1),
    // 业务需求
    BIZ_DEMAND(2),
    // 任务
    TASK(3),
    // 提测单-冒烟用例
    TEST_BILL_CASE(4),
    // 提测单-自测通过
    TEST_BILL_PASS(5),
    // 线下bug
    BUG_OFFLINE(6),
    // 线上bug
    BUG_ONLINE(7),
    // 故障单
    TROUBLE_TICKET(8),
    // 详设评审
    TECH_REVIEW(9)
    ;

    private Integer code;

}
