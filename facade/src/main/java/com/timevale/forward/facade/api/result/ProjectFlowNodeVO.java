package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author yexuan
 * @date 2022-07-21 16:39 yexuan
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点流程详情")
public class ProjectFlowNodeVO extends ToString {

    @ApiModelProperty("状态:-1撤回,0审核中,1审核通过,2审核不通过")
    private Integer projectFlowStatus;

    @ApiModelProperty("项目节点id")
    @JsonSerialize(using = ToStringSerializer.class)
    private  Long projectNodeId;

    @ApiModelProperty("流程项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectFlowId;
}
