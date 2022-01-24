package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-数据指标")
public class HomePageDataIndicatorVO extends ToString {

    @ApiModelProperty("项目总数")
    private Integer projectCount;

    @ApiModelProperty("已上线项目占比")
    private BigDecimal onlineProjectRate;

    @ApiModelProperty("P0P1项目总数")
    private Integer projectCountP0P1;

    @ApiModelProperty("已上线P0P1项目占比")
    private BigDecimal onlineProjectRateP0P1;

    @ApiModelProperty("逾期项目")
    private Integer overdueProjectCount;

    @ApiModelProperty("逾期项目占比")
    private BigDecimal overdueProjectRate;

    @ApiModelProperty("需求总数")
    private Integer productDemandCount;

    @ApiModelProperty("已上线需求占比")
    private BigDecimal onlineProductDemandRate;

}
