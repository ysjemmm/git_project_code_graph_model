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
@ApiModel("线下bug变更项目")
public class BugOfflineChangeProjectReq extends BaseReq {

    @ApiModelProperty("线下bug id列表")
    @NotNull(message = "线下bug id不能为空")
    private List<Long> ids;

    @ApiModelProperty(value = "目标项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "目标产品线id")
    @NotNull(message = "产品线id不能为空")
    private Long productLineId;

}