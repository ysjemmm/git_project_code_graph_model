package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class TaskDO extends BaseDO {

    /**
     * name
     */
    private String name;
    /**
     * projectId
     */
    private Long projectId;
    /**
     * 阶段:0需求规划阶段,1研发阶段,2测试阶段
     */
    private Integer stage;

    /**
     * 0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废
     */
    private Integer status;

    /**
     * 项目计划开始时间
     */
    private Date planStartDate;
    /**
     * 项目计划结束时间
     */
    private Date planEndDate;

    /**
     * 项目实际开始时间
     */
    private Date actualStartDate;
    /**
     * 项目实际结束时间
     */
    private Date actualEndDate;
    /**
     * 产品需求id
     */
    private List<Long> productDemandIds;

    /**
     * 创建待办
     */
    private Boolean todo;
    /**
     * 待办id
     */
    private String todoId;

}
