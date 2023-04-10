package com.timevale.forward.dal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
public class TaskBoardDTO {
    /**
     * 主键
     */
    private Long id;
    /**
     * name
     */
    private String name;
    /**
     * projectId
     */
    private Long projectId;

    /**
     * 0待执行、10进行中、20已完成、-10已暂停、-20已作废
     */
    private Integer status;

    /**
     * 计划开始时间
     */
    private Date planStartDate;
    /**
     * 计划结束时间
     */
    private Date planEndDate;

    /**
     * 项目计划结束时间
     */
    private Date projectPlanEndDate;
    /**
     * 计划耗时
     */

    private BigDecimal planUseTime;

    /**
     * 项目实际开始时间
     */
    private Date actualStartDate;
    /**
     * 项目实际结束时间
     */
    private Date actualEndDate;

    /**
     * 任务耗时
     */

    private BigDecimal taskUseTime;

    /**
     * startDate
     */
    private Date startDate;

    /**
     * endDate
     */
    private Date endDate;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目类别
     */
    private Integer category;

}
