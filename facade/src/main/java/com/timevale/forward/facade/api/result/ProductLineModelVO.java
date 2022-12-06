package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2021/12/13 16:36
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("产品线-模块信息")
public class ProductLineModelVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("产品线负责人")
    private String owner;

    @ApiModelProperty("产品线负责人id")
    private String ownerId;

    @ApiModelProperty("产品线bug负责人")
    private String bugOnlineOwner;

    @ApiModelProperty("产品线bug负责人id")
    private String bugOnlineOwnerId;

    @ApiModelProperty("模块信息")
    private List<ModelVO> models;

    @ApiModelProperty("是否废弃")
    private Boolean isDeleted;


    @ApiModelProperty("业务域id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bizDomainId;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

}
