package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum ProductDemandStatusEnum {
    /**
     * 0待排期,5项目已暂停,10已列入项目,20项目进行中,30已完成上线,-10已暂停,-20已作废
     */
    WAITING(0, "待排期"),

    PJ_SUSPEND(5, "项目已暂停"),

    INCLUDED(10, "已列入项目"),

    PROGRESS(20, "项目进行中"),

    ONLINE(30, "已完成上线"),

    SUSPEND(-10, "已暂停"),

    INVALID(-20, "已作废"),

    DEVELOPING(15, "研发中"),

    DEV_COMPLETED(25, "研发完成");

    final private Integer code;

    final private String text;

    ProductDemandStatusEnum(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getTextByCode(Integer code) {
        for (ProductDemandStatusEnum e : ProductDemandStatusEnum.values()) {
            if (e.getCode().equals(code)) {
                return e.text;
            }
        }
        return "";
    }

    public static boolean unfinished(Integer code) {
        return WAITING.code.equals(code) ||
                INCLUDED.code.equals(code) ||
                PJ_SUSPEND.code.equals(code) ||
                PROGRESS.code.equals(code) ||
                SUSPEND.code.equals(code);
    }
}
