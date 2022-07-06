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
 * @author by xingyun
 * @date 2022/05/25 16:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点版本比较信息")
public class ProjectNodeRecordCompareVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("小版本")
    private BigDecimal minVersion;

    @ApiModelProperty("大版本")
    private BigDecimal maxVersion;

    @ApiModelProperty("节点名称")
    private String name;

    @ApiModelProperty("小版本计划时间")
    private Date minPlanDate;

    @ApiModelProperty("大版本计划时间")
    private Date maxPlanDate;

    @ApiModelProperty("时差(工作日)天")
    private BigDecimal timeDiff;

}
