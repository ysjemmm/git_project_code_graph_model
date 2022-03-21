package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
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
    @NotBlank(message = "事项概述不能为空")
    private String name;

    @ApiModelProperty("故障工单id")
    private Long troubleTicketId;

    @ApiModelProperty("执行人")
    @NotBlank(message = "执行人不能为空")
    private String executor;

    @ApiModelProperty("执行人id")
    @NotBlank(message = "执行人id为空")
    private String executorId;

    @ApiModelProperty("落实日期")
    @NotNull(message = "落实日期不能为空")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date implementationTime;

    @ApiModelProperty("是否创建待办")
    @NotNull(message = "是否创建待办不能为空")
    private Boolean todo;
}
