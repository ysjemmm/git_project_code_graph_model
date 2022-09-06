package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectDO extends BaseDO {
    /**
     * name
     */
    private String name;

    /**
     * 是否为客户开发项目：0否，1是
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
     * 0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废
     */
    private Integer status;

    /**
     * 0 开始规划,10 需求内审,20 需求串讲,30 技术详设评审,40 开发开始,50 编写测试用例,60 用例评审,70 提测,80 测试开始,90 发布模拟,100 发布正式
     */
    private Integer nodeStatus;

    /**
     * 产品线
     */
    private List<Long> productLineIds;

    /**
     * pm名称
     */
    private String pmName;

    /**
     * pm
     */
    private String pmId;

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
     * 描述
     */
    private String desc;

    /**
     * 产品经理
     */
    private List<PersonDO> pds;

    /**
     * 是否在发布平台发布
     */
    private Integer isPlatformPublish;

    /**
     * 是否有项目目标
     */
    private Integer isWithGoal;

    /**
     * 项目等级
     */
    private Integer level;

    /**
     * 产品资源评估（人天）
     */
    private BigDecimal resourceAssessment;

    /**
     * 立项开始时间
     */
    private Date pjEstablishStartDate;


    /**
     * 立项预期上线时间
     */
    private Date pjEstablishPublishDate;

    /**
     * 是否需要验收
     */
    private Integer isAcceptance;

}
