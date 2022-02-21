package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by YangXu
 * @date 2022/02/21 15:38
 */
@Getter
@AllArgsConstructor
public enum HomePageTabEnum {

    /**
     * 个人
     */
    INDIVIDUAL(0),

    /**
     * 团队
     */
    TEAM(1);

    private Integer code;
}
