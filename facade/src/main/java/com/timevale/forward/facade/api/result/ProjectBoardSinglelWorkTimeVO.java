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
 * @author by xingyun
 * @date 2022/05/25 16:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目维度看板-单人人员工期")
public class ProjectBoardSinglelWorkTimeVO extends ToString {

    @ApiModelProperty("执行人")
    private String executor;

    @ApiModelProperty("执行人id")
    private String executorId;

    @ApiModelProperty("是否pm")
    private Boolean isPm;

    @ApiModelProperty("任务数量")
    private Integer taskCount;

    @ApiModelProperty("任务计划总耗时")
    private BigDecimal totalPlanUseTime;

    @ApiModelProperty("任务实际总耗时")
    private BigDecimal totalTaskUseTime;

    @ApiModelProperty("项目开始时间")
    private Date projectStartDate;

    @ApiModelProperty("项目结束时间")
    private Date projectEndDate;

    @ApiModelProperty("项目任务")
    private List<ProjectBoardTaskVO> projectBoardTaskVos;


}
