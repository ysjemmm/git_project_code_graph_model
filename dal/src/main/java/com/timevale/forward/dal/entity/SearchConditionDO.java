package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/04/21 17:33
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SearchConditionDO extends BaseDO{
    /**
     * 名字
     */
    private String name;

    /**
     * 所属模块:0 业务需求，10 产品需求， 20 项目管理， 30 任务管理， 40 线下bug， 50 线上bug， 60 故障单
     */
    private Integer model;

    /**
     * tab：0 全部， 10 我的， 20 我接收的， 30 抄送我的， 40 我下属的， 50 我团队的， 60 我团结提交的， 70 我团队接收的， 80 我部门的
     */
    private Integer tabType;

    /**
     * 内容json
     */
    private String content;

    /**
     * 是否为默认查询条件默认
     */
    private Boolean isDefault;

    /**
     * 属于人
     */
    private String belongMan;

    /**
     * 属于人id
     */
    private String belongManId;
}
