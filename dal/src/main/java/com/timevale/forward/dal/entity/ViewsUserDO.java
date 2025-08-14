package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 视图用户表
 * 对应表：views_user
 * @author by qiyuan
 * @date 2025/08/14 14:42
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ViewsUserDO extends BaseDO {
    /**
     * 项目id
     */
    private Long viewsId;

    /**
     * 类型：0-自己创建的，1-他人分享的
     */
    private Integer type;

    /**
     * 相对位置
     */
    private BigDecimal position;

    /**
     * 0-显示，1-隐藏
     */
    private Boolean hidden;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 负责人id
     */
    private String ownerId;

}