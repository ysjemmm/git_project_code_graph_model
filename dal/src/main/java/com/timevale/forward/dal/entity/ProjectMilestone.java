package com.timevale.forward.dal.entity;

import lombok.Data;

import java.util.Date;

/**
 * 项目里程碑
 * @TableName project_milestone
 */
@Data
public class ProjectMilestone {
    /**
     * 主键id
     */
    private Long id;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 里程碑名称
     */
    private String milestoneName;

    /**
     * 里程碑类型0-任务;1-项目
     */
    private Integer type;

    /**
     * 文档所属项目阶段:11:启动阶段;12:规划阶段;13:执行阶段;14:收尾阶段;15:运营阶段
     */
    private Integer stage;

    /**
     * 关联数据id
     */
    private Long relationId;

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

}