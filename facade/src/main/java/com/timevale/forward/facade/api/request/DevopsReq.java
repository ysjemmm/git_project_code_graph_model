package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2023/02/24 16:30
 */
@Getter
@Setter
@ApiModel("发布平台请求接口")
public class DevopsReq extends ToString {

    @ApiModelProperty(value = "主体id")
    @NotNull(message = "主体id必填")
    private Long mainId;

    @ApiModelProperty(value = "项目标识")
    @NotBlank(message = "项目标识必填")
    private String devopsProjectSign;
}
