package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2021/12/27 18:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求接收请求")
public class BizDemandAgreeReq extends BaseReq {

    @ApiModelProperty("业务需求id")
    @NotNull(message = "业务需求id不能为空")
    private Long bizDemandId;

    @ApiModelProperty("计划上线时间")
    @NotNull(message = "计划上线时间不能为空")
    private Integer planReleaseDate;
}
