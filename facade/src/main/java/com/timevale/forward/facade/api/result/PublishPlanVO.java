package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("发布计划信息")
public class PublishPlanVO extends ToString {

    @ApiModelProperty("发布计划id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("发布计划名称")
    private String name;

    @ApiModelProperty("应用名称")
    private List<String> appNames;

    @ApiModelProperty("发布窗口开始时间")
    private Date windowStart;

    @ApiModelProperty("发布窗口结束时间")
    private Date windowEnd;

    @ApiModelProperty("审批状态")
    private String status;

    @ApiModelProperty("发布状态")
    private String releaseStatus;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("是否紧急发布")
    private Boolean emergency;

    @ApiModelProperty("是否已被关联")
    private Boolean isLinked;
}
