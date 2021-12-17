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
    private Date planDate;
    /**
     * 实际完成时间
     */
    private Date actualDate;

}
