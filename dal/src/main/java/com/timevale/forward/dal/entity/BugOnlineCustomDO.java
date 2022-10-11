package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Description:
 * @ClassName: BugOnlineCustomDO
 * @Author yexuan
 * @Date  2022-09-23 15:20
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOnlineCustomDO extends BaseDO {
    /**
     * 客户名称
     */
    private String customName;

    /**
     * 客户id
     */
    private Long customId;

    /**
     * 线上bug id
     *
     */
    private Long bugOnlineId;
}