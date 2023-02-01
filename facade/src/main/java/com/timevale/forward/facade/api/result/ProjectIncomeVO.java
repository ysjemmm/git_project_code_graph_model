package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
public class ProjectIncomeVO extends ToString {

    @ApiModelProperty(value = "收益id")
    private Long id;

    @ApiModelProperty(value = "项目id")
    private Long projectId;

    @ApiModelProperty("收益金额")
    private BigDecimal incomeAmount;

    @ApiModelProperty("收益日期")
    private Date incomeDate;

    @ApiModelProperty("收益情况")
    private String situation;

}
