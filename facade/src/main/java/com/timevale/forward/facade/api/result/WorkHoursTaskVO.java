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
import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:46
 * @description: 工时记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("任务工时记录")
public class WorkHoursTaskVO extends ToString {

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("工作项类别")
    private Integer workItemType;

    @ApiModelProperty("工作项id")
    private Long workItemId;

    @ApiModelProperty("工作项名称")
    private String workItemName;

    @ApiModelProperty("工时记录")
    private List<WorkHoursRecordVO> workHoursRecords;

    @ApiModelProperty("预估工时")
    private BigDecimal estimateHours;

    @ApiModelProperty("总工时")
    private BigDecimal totalHours;
}
