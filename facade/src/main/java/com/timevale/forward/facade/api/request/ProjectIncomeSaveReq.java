package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
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
@ApiModel("项目收益新增入参")
public class ProjectIncomeSaveReq extends ToString {

    @ApiModelProperty(value = "项目id")
    private Long projectId;

    @ApiModelProperty("收益金额")
    private BigDecimal incomeAmount;

    @ApiModelProperty("收益日期")
    private Date incomeDate;

    @ApiModelProperty("收益情况")
    private String situation;

}
