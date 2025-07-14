package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @auther: yuhua
 * @date: 2025/7/2 17:46
 * @description: 工时记录
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时记录")
public class WorkHoursRecordVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("工作项类别")
    private Integer workItemType;

    @ApiModelProperty("工作项id")
    private Long workItemId;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改时间")
    private Date modifyDate;

    @ApiModelProperty("进度")
    private Integer progress;

    @ApiModelProperty("工时")
    private BigDecimal workHours;
}
