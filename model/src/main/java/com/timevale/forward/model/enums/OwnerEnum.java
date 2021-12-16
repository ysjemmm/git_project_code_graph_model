package com.timevale.forward.model.enums;

public enum OwnerEnum {
    
    CURRENT_USER("我的"),
    FOLLOWER("我下属的"),
    TEAM("我团队的"),
    DEPARTMENT("我部门的"),
    COPIER("抄送我的"),
    RECEIVE("我接收的"),
    ALL("全部");

    private final String text;
    OwnerEnum(String text){
        this.text = text;
    }
}
