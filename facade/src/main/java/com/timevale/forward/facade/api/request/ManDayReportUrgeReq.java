package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;


/**
 * @author by YangXu
 * @date 2022/08/23 18:07
 */
@Getter
@Setter
@ApiModel("人天提报催办请求")
public class ManDayReportUrgeReq extends BaseReq {

    @NotNull(message = "提报id必填")
    @ApiModelProperty("提报id")
    private Long id;
}
