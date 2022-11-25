package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目文档未填写原因")
public class ProjectUnWriteReasonModifyReq extends BaseReq {
    
    @ApiModelProperty("id")
    @NotNull(message = "项目id不能为空")
    private Long id;

    @ApiModelProperty("文档未填写原因")
    @NotNull(message = "文档未填写原因不能为空")
    private String unWriteReason;
}
