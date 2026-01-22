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
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点")
public class ProjectNodeVO extends ToString {
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("计划完成时间")
    private Date planDate;

    @ApiModelProperty("计划完成结束时间")
    private Date planEndDate;

    @ApiModelProperty("实际完成时间")
    private Date actualDate;

    @ApiModelProperty("实际完成结束时间")
    private Date actualEndDate;

    @ApiModelProperty("阶段类型")
    private String stageType;

    @ApiModelProperty("阶段名称")
    private String stageName;

}
