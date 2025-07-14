package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:46
 * @description: 工时进度
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时进度")
public class WorkHoursProgressVO extends ToString {

    @ApiModelProperty("总预估工时")
    private BigDecimal totalEstimatedHours;

    @ApiModelProperty("已登记工时")
    private BigDecimal totalManHour;

    @ApiModelProperty("总剩余工时")
    private BigDecimal totalRemainingHours;

    @ApiModelProperty("工时进度")
    private BigDecimal totalTimeProgress;

    @ApiModelProperty("预估偏差")
    private BigDecimal totalEstimateVariance;

}
