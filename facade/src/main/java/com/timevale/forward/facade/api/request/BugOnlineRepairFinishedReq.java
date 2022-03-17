package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/3/17 15:35
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug修复完毕")
public class BugOnlineRepairFinishedReq extends BaseReq {
    @ApiModelProperty("线上bug id")
    @NotNull(message = "线上bug id不能为空")
    private Long id;

    @ApiModelProperty("经办人:格式 花名-真名")
    @NotNull(message = "经办人不能为空")
    private String operator;

    @ApiModelProperty("经办人花名拼音")
    @NotNull(message = "经办人花名拼音不能为空")
    private String operatorId;
}