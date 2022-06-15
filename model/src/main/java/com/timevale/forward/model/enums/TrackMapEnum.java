package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/16 10:18
 */
@Getter
@AllArgsConstructor
public enum TrackMapEnum {
    // 埋点页面
    PAGE(4),
    // 埋点元素
    ELEMENT(5)
    ;
    private Integer code;

}
