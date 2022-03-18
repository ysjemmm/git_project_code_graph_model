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
 * @date 2022/03/16 14:43
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("故障工单列表")
public class TroubleTicketVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("故障概述")
    private String name;

    @ApiModelProperty("故障定级")
    private Integer troubleRank;

    @ApiModelProperty("故障定级描述")
    private String troubleRankName;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("提出人")
    private String createMan;

    @ApiModelProperty("提出人id")
    private String createManId;

    @ApiModelProperty("处理人")
    private String handler;

    @ApiModelProperty("处理人id")
    private String handlerId;

    @ApiModelProperty("故障发生时间")
    private Date occurrenceTime;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

}
