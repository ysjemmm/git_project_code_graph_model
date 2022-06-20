package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/15 17:13
 @Getter
 */
@Getter
public enum TrackStatusEnum {
    /**
     * 项目流程状态
     */
    WITHDRAW(-1, "已撤回"),
    REVIEWING(0, "审核中"),
    REVIEWED(1, "审核通过"),
    REVIEW_FAIL(2, "审核不通过");
    private final Integer code;
    private final String text;

    TrackStatusEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code){
        for (TrackStatusEnum e : TrackStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "";
    }

    public static boolean canDelete(Integer code){
        return WITHDRAW.getCode().equals(code)||REVIEW_FAIL.getCode().equals(code);
    }
}
