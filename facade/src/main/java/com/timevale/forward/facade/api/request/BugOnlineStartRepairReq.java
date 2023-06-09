package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @Date 2022/3/21 13:44
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug开始修复")
public class BugOnlineStartRepairReq extends BaseReq {
    @ApiModelProperty("线上bug id")
    @NotNull(message = "线上bug id不能为空")
    private Long id;

    @ApiModelProperty("bug原因")
    @NotNull(message = "bug原因不能为空")
    private Integer reason;

    @ApiModelProperty("归因阶段")
    @NotNull(message = "归因阶段不能为空")
    private Integer reasonStage;

    @ApiModelProperty("问题原因")
    @NotNull(message = "问题原因不能为空")
    private String problemReason;

    @ApiModelProperty("bug修复方案")
    @NotNull(message = "bug修复方案不能为空")
    private String solveScheme;

    @ApiModelProperty("预计上线日期")
    @NotNull(message = "预计上线日期不能为空")
    private Date expectLaunchDate;

    @ApiModelProperty("线下bugId")
    private Long bugOfflineId;

    @ApiModelProperty("用户临时解决方案")
    private String temporarySolution;
}