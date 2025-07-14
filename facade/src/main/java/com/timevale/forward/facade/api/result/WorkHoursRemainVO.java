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
 * @description: 工时剩余
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时剩余")
public class WorkHoursRemainVO extends ToString {

    @ApiModelProperty("预估工时")
    private BigDecimal estimatedHours;

    @ApiModelProperty("填报总工时")
    private BigDecimal totalManHour;

    @ApiModelProperty("剩余工时")
    private BigDecimal remainingManHour;

    @ApiModelProperty("当日剩余可登记工时")
    private BigDecimal remainingHourDeviation;

}
