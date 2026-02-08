package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 单独更新项目节点请求
 *
 * @author forward
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点单独更新")
public class ProjectNodeUpdateReq extends BaseReq {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty("节点列表")
    @NotEmpty(message = "节点列表不能为空")
    @Valid
    private List<ProjectNodeAddReq> projectNodes;
}
