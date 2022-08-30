package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("任务批量新增")
public class TaskBatchAddReq extends BaseReq {

    @ApiModelProperty("任务信息")
    @NotNull(message = "任务信息不能为空")
    @Valid
    private List<TaskSimpleAddReq> taskSimples;

}
