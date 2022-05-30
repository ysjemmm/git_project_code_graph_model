package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目维度看板-任务")
public class ProjectBoardTaskVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("项目状态:0待执行、10进行中、20已完成、-10已暂停、-20已作废")
    private Integer status;

    @ApiModelProperty("项目状态")
    private String statusName;

    @ApiModelProperty("是否逾期")
    private Boolean isDelay;

    @ApiModelProperty("执行人")
    private String executor;

    @ApiModelProperty("执行人id")
    private String executorId;

    @ApiModelProperty("计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("计划耗时")
    private BigDecimal planUseTime;

}
