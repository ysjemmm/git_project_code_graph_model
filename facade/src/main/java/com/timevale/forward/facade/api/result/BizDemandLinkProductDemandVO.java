package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/23 13:55
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求关联产品需求搜索展示")
public class BizDemandLinkProductDemandVO extends ToString {

    @ApiModelProperty("产品需求主题")
    private String name;

    @ApiModelProperty("产品需求id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private Integer priority;

    @ApiModelProperty("优先级名称")
    private String priorityText;

    @ApiModelProperty("业务域id")
    private Long bizDomainId;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("产品需求负责人")
    private PersonQuery ownerInfo;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("产品需求状态")
    private Integer status;

    @ApiModelProperty("产品需求状态名称")
    private String statusText;
}
