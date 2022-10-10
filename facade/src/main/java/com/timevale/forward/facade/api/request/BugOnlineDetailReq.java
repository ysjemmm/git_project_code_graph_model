package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * @Date 2022/3/17 15:07
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("查看线上bug详情")
public class BugOnlineDetailReq extends BaseReq {
    @ApiModelProperty("线上bug id")
    @NotNull(message = "线上bug id不能为空")
    @Pattern(regexp = "^[1-9]+[0-9]*$", message = "id只能为正整数")
    private String id;
}