package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2021/12/27 18:06
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求已处理（无需开发）请求")
public class BizDemandCompletedReq extends BaseReq {

    @ApiModelProperty("业务需求id")
    @NotNull(message = "业务需求id不能为空")
    private Long id;

    @ApiModelProperty("处理方案")
    @NotBlank(message = "处理方案不能为空")
    private String solvePlan;

    @ApiModelProperty("产品线id")
    @NotNull(message = "产品线不能为空")
    private Long productLineId;
}
