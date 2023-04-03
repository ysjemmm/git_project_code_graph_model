package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/3/17 16:16
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug 已上线")
public class BugOnlineOnlineReq extends BaseReq {
    @ApiModelProperty("线上bug id")
    @NotNull(message = "线上bug id不能为空")
    private Long id;

    @ApiModelProperty("bug原因")
    @NotNull(message = "bug原因必填")
    private Integer reason;

    @ApiModelProperty("归因阶段")
    @NotNull(message = "归因阶段必填")
    private Integer reasonStage;

    @ApiModelProperty("线下bugId")
    private Long bugOfflineId;
}