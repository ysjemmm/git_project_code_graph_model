package com.timevale.forward.dal.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 线上bug-业务需求关系表(N-N)
 * @TableName bug_online_biz_demand
 */
@Data
public class BugOnlineBizDemand implements Serializable {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 线上bugid
     */
    private Long bugOnlineId;

    /**
     * 业务需求id
     */
    private Long bizDemandId;

    /**
     * 删除标记
     */
    private Integer isDeleted;

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

    private static final long serialVersionUID = 1L;
}