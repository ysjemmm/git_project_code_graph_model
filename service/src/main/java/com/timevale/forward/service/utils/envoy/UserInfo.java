package com.timevale.forward.service.utils.envoy;

import com.timevale.forward.service.constant.CommonConstant;
import lombok.Data;

import java.util.List;

/**
 * @author jingchun
 * created on 2021/10/21
 */
@Data
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

    /**
     * 默认部门
     */
    private GroupModel defaultGroup;

    public String getFullAlias() {
        return alias + CommonConstant.JOIN_LINE + name;
    }

}
