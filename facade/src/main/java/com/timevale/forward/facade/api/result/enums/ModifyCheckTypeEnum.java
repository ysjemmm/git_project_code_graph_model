package com.timevale.forward.facade.api.result.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author jingchun
 * create on 8/1/2023
 **/
@Getter
@AllArgsConstructor
public enum ModifyCheckTypeEnum {
    // 0. 无处理页面的情况，提示信息
    ANY(false),
    // 1. 无处理页面且必须处理的情况
    ANY_CRITICAL(true),
    // 2. 验收单校验
    ACCEPTANCE(true),
    // 3. 发布计划校验
    PUBLISH(false),
    // 4. 文档未维护
    DOCUMENT(false),
    // 5. 人天未维护
    MAN_DAY(false),
    // 6. 存在BUG未关闭
    BUG_OFFLINE(true);

    /**
     * 是否强制校验
     */
    private final boolean critical;

}
