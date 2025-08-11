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
import java.util.List;

/**
 * 产品需求分组VO
 * @author qiyuan
 * @date 2025/07/14 15:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组")
public class ProductDemandGroupVO extends ToString {

    @ApiModelProperty("主键id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("业务域id")
    private Long bizDomainId;

    @ApiModelProperty("相对位置")
    private BigDecimal position;

    @ApiModelProperty("版本号")
    private Long version;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("负责人id")
    private String ownerId;

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目状态")
    private Integer status;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("删除标记")
    private Boolean isDeleted;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改人id")
    private String modifyManId;

    @ApiModelProperty("修改人")
    private String modifyMan;

    @ApiModelProperty("修改时间")
    private Date modifyDate;

    @ApiModelProperty("产品需求分组需求列表")
    private List<ProductDemandGroupItemVO> productDemandGroupItems;
} 