package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 20:17
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求新增")
public class ProductDemandAddReq extends BaseReq {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    private Byte priority;

    @ApiModelProperty("产品线")
    private Long productLineId;

    @ApiModelProperty("类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求")
    private Byte type;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipients;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;


}
