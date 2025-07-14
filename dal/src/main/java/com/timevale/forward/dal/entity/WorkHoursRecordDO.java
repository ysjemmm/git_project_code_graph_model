package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:39
 * @description: 工时记录表
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WorkHoursRecordDO extends BaseDO {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 描述
     */
    private String desc;

    /**
     * 工作项类别
     */
    private Integer workItemType;

    /**
     * 工作项id
     */
    private Long workItemId;

    /**
     * 创建人id
     */
    private String createManId;

    /**
     * 创建人
     */
    private String createMan;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 修改人id
     */
    private String modifyManId;

    /**
     * 修改人
     */
    private String modifyMan;

    /**
     * 修改时间
     */
    private Date modifyDate;

    /**
     * 进度
     */
    private Integer progress;

    /**
     * 工时
     */
    private BigDecimal workHours;

}
