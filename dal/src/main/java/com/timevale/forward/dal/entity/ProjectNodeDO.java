package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author: xingyun
 * @create: 2021-12-16 16:01
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectNodeDO extends BaseDO {

    /**
     * projectId
     */
    private Long projectId;
    
    /**
     * name
     */
    private String name;
    
    /**
     * 计划完成时间
     */
    private Date plantDate;
    /**
     * 实际完成时间
     */
    private Date actualDate;

}
