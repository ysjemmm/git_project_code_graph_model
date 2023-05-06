package com.timevale.forward.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * @author by YangXu
 * @date 2023/05/06 11:20
 */
@Getter
@AllArgsConstructor
public enum BugOnlineConvertBizStatusEnum {
    NONE(0,"无",0),
    APPLY(1,"申请",1),
    AGREE(2,"同意",2),
    REJECT(3,"拒绝",3);

    private final Integer code;
    private final String text;
    private final Integer operate;

    public static BugOnlineConvertBizStatusEnum getByOperate(Integer operate) {
        for (BugOnlineConvertBizStatusEnum value : BugOnlineConvertBizStatusEnum.values()) {
            if (value.operate.equals(operate)) {
                return value;
            }
        }
        return null;
    }
}
