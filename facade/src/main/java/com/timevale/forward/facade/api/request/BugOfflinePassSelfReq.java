package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @Date 2022/2/28 17:00
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug自测通过请求")
public class BugOfflinePassSelfReq extends BaseReq {
    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    private Long id;

    @ApiModelProperty("bug产生原因")
    @NotBlank(message = "bug产生原因不能为空")
    private String cause;

    @ApiModelProperty("解决方案")
    @NotBlank(message = "解决方案不能为空")
    private String solvePlan;
}