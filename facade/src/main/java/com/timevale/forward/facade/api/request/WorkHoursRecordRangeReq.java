package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @auther: yuhua
 * @date: 2025/7/3 17:59
 * @description: 工时记录时间范围查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时记录时间范围查询")
public class WorkHoursRecordRangeReq extends BaseReq {

    @ApiModelProperty("查询开始日期")
    @NotNull(message = "查询开始日期不能为空")
    private String startDate;

    @ApiModelProperty("查询结束日期")
    @NotNull(message = "查询结束日期不能为空")
    private String endDate;
}
