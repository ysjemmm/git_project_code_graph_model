package com.timevale.forward.service.utils.envoy;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingchun
 * created on 2021/10/21
 */
@Getter
@Setter
public class UserInfo {
    /**
     * 花名拼音
     */
    private String id;

    /**
     * 姓名
     */
    private String name;

    /**
     * 花名
     */
    private String alias;

    /**
     * 所在部门列表
     */
    private List<GroupModel> groupList;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 邮箱
     */
    private String mail;

    /**
     * 职位
     */
    private String job;
}
