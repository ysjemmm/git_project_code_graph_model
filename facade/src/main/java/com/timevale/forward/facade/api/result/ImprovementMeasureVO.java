package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/03/16 15:21
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("改进措施")
public class ImprovementMeasureVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("事项概述")
    private String name;

    @ApiModelProperty("责任人")
    private String executor;

    @ApiModelProperty("责任人id")
    private String executorId;

    @ApiModelProperty("落实日期")
    private Date implementationTime;

    @ApiModelProperty("是否已建待办")
    private Boolean todo;

    @ApiModelProperty("状态 0 待处理，1 已完成")
    private Integer status;

    @ApiModelProperty("状态-描述")
    private String statusName;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

}
