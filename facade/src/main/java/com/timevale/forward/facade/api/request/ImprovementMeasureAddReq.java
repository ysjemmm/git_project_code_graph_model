package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/03/16 15:21
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("改进措施-新增")
public class ImprovementMeasureAddReq extends BaseReq {

    @ApiModelProperty("事项概述")
    private String name;

    @ApiModelProperty("故障工单id")
    @NotNull(message = "故障工单id")
    private Long troubleTicketId;

    @ApiModelProperty("执行人")
    private String executor;

    @ApiModelProperty("执行人id")
    private String executorId;

    @ApiModelProperty("落实日期")
    private Date implementationTime;

    @ApiModelProperty("是否创建待办")
    private Boolean todo;
}
