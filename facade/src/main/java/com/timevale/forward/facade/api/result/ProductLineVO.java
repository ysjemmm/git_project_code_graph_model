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
 * @date 2021/12/13 16:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("产品线")
public class ProductLineVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("业务域id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizDomainId;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("产品线负责人")
    private String productLineOwner;

    @ApiModelProperty("产品线负责人id")
    private String productLineOwnerId;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("业务域负责人")
    private String bizDomainOwner;

    @ApiModelProperty("业务域负责人id")
    private String bizDomainOwnerId;

    @ApiModelProperty("产品线bug负责人")
    private String bugOnlineOwner;

    @ApiModelProperty("产品线bug负责人id")
    private String bugOnlineOwnerId;

    @ApiModelProperty("sr专家")
    private String srExpert;

    @ApiModelProperty("sr专家id")
    private String srExpertId;

    @ApiModelProperty("是否废弃")
    private Boolean isDeleted;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改时间")
    private Date modifyDate;
}
