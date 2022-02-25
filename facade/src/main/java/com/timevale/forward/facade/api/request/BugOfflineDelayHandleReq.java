package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/2/25 10:18
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug延期修复")
public class BugOfflineDelayHandleReq extends BaseReq {

    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    private Long id;

    @ApiModelProperty("延期修复原因")
    @NotNull(message = "延期修复原因不能为空")
    private String delayHandleReason;
}