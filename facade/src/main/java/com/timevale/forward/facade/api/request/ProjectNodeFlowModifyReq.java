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
@ApiModel("项目节点流程撤回")
public class ProjectNodeFlowModifyReq extends BaseReq {

    @ApiModelProperty("流程id")
    @NotNull(message = "流程id不能为空")
    private Long nodeFlowId;


}
