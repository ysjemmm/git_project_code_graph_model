package com.timevale.forward.model.enums;

import lombok.Getter;

/**
 * @author by YangXu
 * @date 2021/12/17 17:00
 */
@Getter
public enum AscriptionEnum {
    /**
     * 页面权限归属
     */
    CURRENT_USER("我的"),
    FOLLOWER("我下属的"),
    TEAM("我团队的"),
    TEAM_SUBMIT("我团队提交的"),
    TEAM_RECEIVE("我团队接收的"),
    DEPARTMENT("我部门的"),
    COPIER("抄送我的"),
    RECEIVE("我接收的"),
    ALL("全部"),
    ;

    private final String text;
    AscriptionEnum(String text){
        this.text = text;
    }
}