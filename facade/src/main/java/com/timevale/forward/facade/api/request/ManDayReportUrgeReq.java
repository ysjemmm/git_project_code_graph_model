package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author jingchun
 * create on 2022/6/20
 */
@Getter
@Setter
@ApiModel("人天提报修改请求")
public class ManDayReportUrgeReq extends BaseReq {

    @NotNull(message = "提报id必填")
    @ApiModelProperty("提报id")
    private Long id;
}
