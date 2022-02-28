package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/2/28 17:00
 * @Author 望轩
 */
@Data
@ApiModel("线下bug")
public class BugOfflineReq {
    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    private Long id;
}