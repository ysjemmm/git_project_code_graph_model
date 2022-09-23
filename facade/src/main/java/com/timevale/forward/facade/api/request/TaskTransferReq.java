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
@ApiModel("任务转移项目")
public class TaskTransferReq extends BaseReq {

    @ApiModelProperty("任务id")
    @NotNull(message = "任务id不能为空")
    private List<Long> ids;

    @ApiModelProperty(value = "项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "产品线id")
    @NotNull(message = "产品线id不能为空")
    private Long productLineId;

}
