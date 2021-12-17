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
public class ProjectDO extends BaseDO {
    /**
     * name
     */
    private String name;

    /**
     * 优先级:0(P0),1(P1),2(P2),3(P3)
     */
    private Byte priority;
    /**
     * 类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求
     */
    private Byte type;

    /**
     * 0待启动,10规划中,20研发中,30测试中,40已发布,50已暂停,60已作废
     */
    private Byte status;

    /**
     * 产品线
     */
    private List<Long> productLineIds;
    
    /**
     * pm
     */
    private String pm;
    
    /**
     * 项目计划开始时间
     */
    private Date planStartDate;
    /**
     * 项目计划结束时间
     */
    private Date planEndDate;

    /**
     * 描述
     */
    private String desc;

}
