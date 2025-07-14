package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @auther: yuhua
 * @date: 2025/7/4 17:11
 * @description: 工时记录修改
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时记录修改")
public class WorkHoursRecordModifyReq extends WorkHoursRecordAddReq {
    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    private Long id;
}
