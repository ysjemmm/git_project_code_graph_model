package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 视图用户列表
 * @author by qiyuan
 * @date 2025/08/14 14:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ViewsUserListDO extends BaseDO {
    /**
     * 视图id
     */
    private Long viewsId;

    /**
     * 业务类型：10-业务需求，11-产品需求，12-项目，13-线下bug，14-线上bug
     */
    private Integer viewsType;

    /**
     * 0-显示，1-隐藏
     */
    private Boolean hidden;

    /**
     * 相对位置
     */
    private BigDecimal position;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人id
     */
    private String ownerId;


    /**
     * 分组字段
     */
    private String groupField;

    /**
     * 筛选条件
     */
    private String filterCondition;

    /**
     * 视图名称
     */
    private String name;

    private List<String> shareUsers;

}