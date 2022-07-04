package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点延期检查")
public class ProjectNodeFlowCheckReq extends BaseReq {

    @ApiModelProperty("项目节点")
    @NotNull(message = "项目节点不能为空")
    private List<ProjectNodeAddReq> projectNodeAddReq;

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;
}
