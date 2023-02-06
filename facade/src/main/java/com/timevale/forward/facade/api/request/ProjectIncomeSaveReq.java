package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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

    @ApiModelProperty(value = "收益id")
    private Long id;

    @NotNull(message = "项目id必填")
    @ApiModelProperty(value = "项目id")
    private Long projectId;

    @NotNull(message = "项目金额必填")
    @ApiModelProperty("收益金额")
    private BigDecimal incomeAmount;

    @NotNull(message = "收益日期必填")
    @ApiModelProperty("收益日期")
    private Date incomeDate;

    @NotBlank(message = "收益情况必填")
    @Length(max = 100, message = "收益情况长度限制在100以内")
    @ApiModelProperty("收益情况")
    private String situation;

}
