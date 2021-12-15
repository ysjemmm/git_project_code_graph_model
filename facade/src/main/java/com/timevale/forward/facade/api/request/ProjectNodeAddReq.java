package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author: xingyun
 * @create: 2021-12-15 16:23
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目节点新增")
public class ProjectNodeAddReq extends BaseReq{

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("计划完成时间")
    private Date plantDate;

    @ApiModelProperty("实际完成时间")
    private Date actualDate;
}
