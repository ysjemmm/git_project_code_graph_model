package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;


/**
 * @author by YangXu
 * @date 2023/02/10 16:38
 */
@Getter
@Setter
@ApiModel("项目结项请求")
public class ProjectConclusionReq extends ToString {
    @NotNull(message = "项目id必填")
    @ApiModelProperty("项目id")
    private Long projectId;

    @NotNull(message = "项目最终状态必填")
    @ApiModelProperty("项目最终状态")
    private Integer targetStatus;

    @ApiModelProperty("作废原因")
    private String invalidReason;
}
