package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @auther: yuhua
 * @date: 2025/7/3 17:59
 * @description: 工时记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时记录新增")
public class WorkHoursRecordAddReq extends BaseReq {

    @ApiModelProperty(value = "项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "描述")
    private String desc;

    @ApiModelProperty(value = "工作项类别")
    @NotNull(message = "工作项类别不能为空")
    private Integer workItemType;

    @ApiModelProperty(value = "工作项id")
    @NotNull(message = "工作项id不能为空")
    private Long workItemId;

    @ApiModelProperty("实际工时")
    @NotNull(message = "实际工时不能为空")
    @Min(value = 0, message = "实际工时不能小于0小时")
    @Max(value = 24, message = "实际工时不能大于24小时")
    private BigDecimal workHours;

    @ApiModelProperty("进度")
    private Integer progress;
}
