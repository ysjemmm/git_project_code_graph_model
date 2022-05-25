package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2022/05/25 16:47
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目维度看板查询")
public class ProjectBoardReq extends BaseReq{

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;
}
