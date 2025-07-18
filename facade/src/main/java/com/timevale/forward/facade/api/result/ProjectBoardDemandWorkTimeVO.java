package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * @auther: yuhua
 * @date: 2025/7/15 10:36
 * @description: 需求任务工期
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目维度看板-需求任务工期")
public class ProjectBoardDemandWorkTimeVO extends ToString {

    @ApiModelProperty("需求")
    private String demand;

    @ApiModelProperty("需求id")
    private Long demandId;

    @ApiModelProperty("需求负责人")
    private String owner;

    @ApiModelProperty("需求负责人id")
    private String ownerId;

    @ApiModelProperty("业务域")
    private String bizDomainName;

    @ApiModelProperty("产品线")
    private String productLineName;

    @ApiModelProperty("优先级")
    private Integer priority;

    @ApiModelProperty("优先级名称")
    private String priorityName;

    @ApiModelProperty("任务数量")
    private Integer taskCount;

    @ApiModelProperty("需求排期")
    private Date expectScheduleTime;

    @ApiModelProperty("需求状态")
    private String statusName;

    @ApiModelProperty("项目开始时间")
    private Date projectStartDate;

    @ApiModelProperty("项目结束时间")
    private Date projectEndDate;

    @ApiModelProperty("任务计划耗时")
    private BigDecimal totalPlanUseTime;

    @ApiModelProperty("项目任务")
    private List<ProjectBoardTaskVO> projectBoardTaskVos;
}
