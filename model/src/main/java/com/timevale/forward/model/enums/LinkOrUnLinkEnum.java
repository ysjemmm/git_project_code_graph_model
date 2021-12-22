package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
public enum LinkOrUnLinkEnum {
    /**
     * 0:关联
     */
    LINK(0),

    /**
     * 0:取消关联
     */
    UN_LINK(1);

    private final Integer code;
    LinkOrUnLinkEnum(Integer code){
        this.code = code;
    }
}
