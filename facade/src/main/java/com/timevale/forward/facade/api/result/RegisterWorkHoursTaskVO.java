package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:46
 * @description: 登记工时任务列表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("登记工时任务列表")
public class RegisterWorkHoursTaskVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workItemId;

    @ApiModelProperty("projectId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("工作项类别")
    private Integer workItemType;

    @ApiModelProperty("工时日期")
    private String dateStr;

    @ApiModelProperty("预计工时")
    private BigDecimal estimatedHours;

    @ApiModelProperty("优先级")
    private Integer status;

    @ApiModelProperty("优先级名称")
    private String statusName;

    @ApiModelProperty("任务描述")
    private String desc;
}
