package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @auther: yuhua
 * @date: 2025/7/3 17:59
 * @description: 工时记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时记录查询")
public class WorkHoursRecordQueryReq extends BaseReq {

    @ApiModelProperty(value = "项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @ApiModelProperty(value = "工作项类别")
    @NotNull(message = "工作项类别不能为空")
    private Integer workItemType;

    @ApiModelProperty(value = "工作项id")
    @NotNull(message = "工作项id不能为空")
    private Long workItemId;
}
