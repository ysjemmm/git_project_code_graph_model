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
public class ProjectBudgetVO extends ToString {

    @ApiModelProperty("预算id")
    private Long id;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("成本类型")
    private String costType;

    @ApiModelProperty("预计成本金额（元）")
    private BigDecimal expectedAmount;

    @ApiModelProperty("实际已发生成本金额（元）")
    private BigDecimal costAmount;

    @ApiModelProperty("预计成本说明")
    private String expectedDesc;

    @ApiModelProperty("执行金额说明")
    private String costDesc;

    @ApiModelProperty("发生日期")
    private Date costDate;

}
