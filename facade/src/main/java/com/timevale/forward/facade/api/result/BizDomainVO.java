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
 * @date 2021/12/13 16:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务域")
public class BizDomainVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("负责人id")
    private String ownerId;

    @ApiModelProperty("业务域技术负责人")
    private String techOwner;

    @ApiModelProperty("业务域技术负责人id")
    private String techOwnerId;

    @ApiModelProperty("PBU负责人")
    private String pbuOwner;

    @ApiModelProperty("PBU负责人id")
    private String pbuOwnerId;

    @ApiModelProperty("业务需求产品线负责人是否可以直接驳回")
    private Boolean directReject;

    @ApiModelProperty("线上bug是否可以直接转产品需求")
    private Boolean directConvertBiz;

    @ApiModelProperty("上架状态：0-未上架，1-已上架")
    private Integer listingStatus;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改时间")
    private Date modifyDate;
}
