package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/4 17:30
 * @description: 工时记录批量新增
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时记录批量新增")
public class WorkHoursRecordBatchAddReq extends BaseReq {

    @ApiModelProperty("工时记录信息")
    @NotNull(message = "工时记录信息不能为空")
    @Valid
    private List<WorkHoursRecordAddReq> workHoursSimples;

}
