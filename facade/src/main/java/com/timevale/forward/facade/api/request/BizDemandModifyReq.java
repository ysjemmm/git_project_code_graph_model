package com.timevale.forward.facade.api.request;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求修改")
public class BizDemandModifyReq extends BizDemandAddReq {

    @ApiModelProperty("业务需求id")
    @NotNull(message = "业务需求id不能为空")
    private Long id;

    @ApiModelProperty("预期上线时间")
    private Integer planReleaseDate;

    @ApiModelProperty("解决方案")
    private String solvePlan;

    @ApiModelProperty("拒绝原因")
    private String rejectReason;

    @ApiModelProperty("是否通知接收人")
    private Boolean notifyReceiveMan;
}
