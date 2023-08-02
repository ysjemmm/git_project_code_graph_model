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
    // 0. 未知
    NULL,
    // 1. 无处理页面的情况
    ANY,
    // 2. 验收单校验
    ACCEPTANCE,
    // 3. 发布计划校验
    PUBLISH,
    // 4. 文档未维护
    DOCUMENT,
    // 5. 人天未维护
    MAN_DAY,
    // 6. 存在BUG未关闭
    BUG_OFFLINE,
    // 7. 项目积分
    PROJECT_POINT,
    ;

}
