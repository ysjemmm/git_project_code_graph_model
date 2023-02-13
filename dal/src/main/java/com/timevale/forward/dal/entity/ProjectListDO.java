package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectListDO extends BaseDO {
    /**
     * name
     */
    private String name;


    /**
     * 是否客开项目
     */
    private Integer customerDev;


    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    private Integer priority;
    /**
     * 类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求
     */
    private Integer type;

    /**
     * 内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目
     */
    private Integer innerType;

    /**
     * 0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废
     */
    private Integer status;

    /**
     * 节点状态
     */
    private Integer nodeStatus;

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
     * 产品经理
     */
    private String pdName;

    /**
     * pmId
     */
    private String pmId;

    /**
     * pm名称
     */
    private String pm;


    /**
     * 是否在发布平台发布
     */
    private Integer isPlatformPublish;

    /**
     * 项目等级
     */
    private Integer level;

    private Integer category;

}
