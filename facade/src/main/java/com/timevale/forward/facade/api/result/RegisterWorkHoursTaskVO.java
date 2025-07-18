package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:46
 * @description: 登记工时任务列表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@ApiModel("登记工时任务列表")
public class RegisterWorkHoursTaskVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long workItemId;

    @ApiModelProperty("projectId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("工作项类别")
    private Integer workItemType;
}
