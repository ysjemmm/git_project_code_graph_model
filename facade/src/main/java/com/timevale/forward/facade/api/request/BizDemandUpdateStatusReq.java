package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2021/12/27 18:03
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求作废请求")
public class BizDemandUpdateStatusReq extends BaseReq {

    @ApiModelProperty("业务需求id")
    @NotNull(message = "业务需求id不能为空")
    private Long bizDemandId;
}
