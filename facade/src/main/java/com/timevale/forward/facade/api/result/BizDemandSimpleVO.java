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
 * @date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求简要信息")
public class BizDemandSimpleVO extends ToString {

    @ApiModelProperty("业务需求id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("需求解决状态:0待评估，10已接收，13待确认，15已完成无需开发，17已关联产品需求，20已列入项目，30项目进行中，40已完成上线，-10被驳回，-20已作废")
    private Integer status;

    @ApiModelProperty("优先级：0-紧急，10-高，20-中，30低")
    private Integer priority;

    @ApiModelProperty("优先级名称")
    private String priorityText;

    @ApiModelProperty("预计上线时间:0~11 分别对应1~12月,20:暂时无法评估")
    private Integer planReleaseDate;

    @ApiModelProperty("预计上线时间名称")
    private String planReleaseDateText;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("需求接收人")
    private String receiveMan;

    @ApiModelProperty("UED资源评估（人天）")
    private BigDecimal uedTime;

    @ApiModelProperty("后端资源评估（人天）")
    private BigDecimal backTime;

    @ApiModelProperty("前端资源评估（人天）")
    private BigDecimal frontTime;

    @ApiModelProperty("测试资源评估（人天）")
    private BigDecimal qaTime;

    @ApiModelProperty("总资源评估（人天）")
    private BigDecimal totalTime;

}
