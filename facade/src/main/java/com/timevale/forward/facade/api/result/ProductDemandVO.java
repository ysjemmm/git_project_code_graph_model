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
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求列表")
public class ProductDemandVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private Integer priority;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private String priorityName;

    @ApiModelProperty("业务域")
    private String bizDomainName;

    @ApiModelProperty("产品线")
    private String productLineName;

    @ApiModelProperty("类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求")
    private Integer type;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废")
    private Integer status;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,40已暂停,50已作废")
    private String statusName;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("修改时间")
    private Date modifyDate;

    @ApiModelProperty("抄送人")
    private String copier;


}
