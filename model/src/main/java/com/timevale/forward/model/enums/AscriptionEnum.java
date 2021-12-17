package com.timevale.forward.model.enums;

public enum AscriptionEnum {
    /**
     * 页面权限归属
     */
    CURRENT_USER("我的"),
    FOLLOWER("我下属的"),
    TEAM("我团队的"),
    DEPARTMENT("我部门的"),
    COPIER("抄送我的"),
    RECEIVE("我接收的"),
    ALL("全部");

    private final String text;
    AscriptionEnum(String text){
        this.text = text;
    }
}
