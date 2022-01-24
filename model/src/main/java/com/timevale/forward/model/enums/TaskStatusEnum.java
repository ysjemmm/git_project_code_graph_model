package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author: xingyun
 * @create: 2021-12-17 11:10
 **/
@Getter
public enum TaskStatusEnum {
    /**
     * 任务状态:0待执行、10进行中、20已完成、-10已暂停、-20已作废
     */
    WAITING(0,"待启动"),

    PROGRESS(10,"进行中"),

    DONE(20,"已完成"),

    SUSPEND(-10,"已暂停"),

    INVALID(-20,"已作废");

    final private Integer code;

    final private String text;

    TaskStatusEnum(Integer code, String text){
        this.code = code;
        this.text = text;
    }
    public static String getTextByCode(Integer code){
        for (TaskStatusEnum e : TaskStatusEnum.values()){
            if(e.getCode().equals(code)){
                return e.text;
            }
        }
        return "errorCode";
    }
}
