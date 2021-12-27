package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求新增")
public class ProductDemandAddReq extends BaseReq {

    @ApiModelProperty("名称")
    @NotNull(message = "需求名称不能为空")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2),3(P3)")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @ApiModelProperty("产品线")
    @NotNull(message = "产品线不能为空")
    private Long productLineId;

    @ApiModelProperty("类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求")
    @NotNull(message = "需求类型不能为空")
    private Integer type;

    @ApiModelProperty("需求负责人")
    @NotNull(message = "需求负责人不能为空")
    private PersonAddReq demandOwner;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipients;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;


}
