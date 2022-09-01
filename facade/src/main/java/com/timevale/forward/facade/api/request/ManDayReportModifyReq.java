package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("人天提报修改请求")
public class ManDayReportModifyReq extends BaseReq {

    @NotNull(message = "提报id必填")
    @ApiModelProperty("提报id")
    private Long id;

    @NotNull(message = "审核状态必填")
    @ApiModelProperty("审核状态")
    private Integer auditStatus;

    @NotNull(message = "驳回原因不能为null")
    @ApiModelProperty("驳回原因")
    private String rejectReason;
}
