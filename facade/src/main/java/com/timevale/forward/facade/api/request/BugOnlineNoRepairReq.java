package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @Date 2022/3/17 16:34
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug 不用修复")
public class BugOnlineNoRepairReq extends BaseReq {

    @ApiModelProperty("线上bug id")
    @NotNull(message = "线上bug id不能为空")
    private Long id;

    @ApiModelProperty("不用修复原因")
    @NotNull(message = "不用修复原因不能为空")
    private Integer dismissCause;

    @ApiModelProperty("归因阶段")
    @NotNull(message = "归因阶段不能为空")
    private Integer dismissCauseStage;

    @ApiModelProperty("关联的线上bug id")
    private Long linkBugId;

    @ApiModelProperty("临时解决方案")
    private String temporarySolution;
}