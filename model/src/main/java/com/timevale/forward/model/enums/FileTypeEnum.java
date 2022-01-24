package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 10:18
 */
@Getter
public enum FileTypeEnum {
    // 产品需求
    PRODUCT_DEMAND(1),
    // 业务需求
    BIZ_DEMAND(2),
    // 任务
    TASK(3),
    //提测单-冒烟用例
    SMOKING_USE_CASES(4),
    //提测单-自测通过
    SELF_TEST_PASS(5);

    private Integer code;

    FileTypeEnum(Integer code) {
        this.code = code;
    }
}
