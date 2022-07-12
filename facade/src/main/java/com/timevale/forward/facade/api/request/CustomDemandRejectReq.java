package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2021/12/27 18:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("客户需求驳回请求")
public class CustomDemandRejectReq extends BaseReq {

    @ApiModelProperty("客户需求id")
    @NotNull(message = "客户需求id不能为空")
    private Long id;

    @ApiModelProperty("驳回理由")
    @NotNull(message = "驳回理由不能为空")
    private Integer reason;
}
