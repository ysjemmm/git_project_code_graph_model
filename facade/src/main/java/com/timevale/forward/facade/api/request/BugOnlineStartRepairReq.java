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

    @ApiModelProperty("bug原因：0需求问题，1环境配置问题，2功能错误，3兼容性问题，4数据问题，5性能问题，6安全问题" +
            "，7外部原因，8开发误操作，9接口文档编写错误，10外包项目，11历史版本，12无测试参与版本，13测试环境延期未修复bug" +
            "，14设计缺陷，15定制版升级改动波及，16无法重现但客户环境偶现，17无法重现但客户环境必现")
    @NotNull(message = "bug原因不能为空")
    private Integer reason;

    @ApiModelProperty("归因阶段")
    @NotNull(message = "归因阶段不能为空")
    private Integer reasonStage;

    @ApiModelProperty("问题原因")
    @NotNull(message = "问题原因不能为空")
    private String problemReason;

    @ApiModelProperty("解决方案")
    @NotNull(message = "解决方案不能为空")
    private String solveScheme;

    @ApiModelProperty("预计上线日期")
    @NotNull(message = "预计上线日期不能为空")
    private Date expectLaunchDate;
}