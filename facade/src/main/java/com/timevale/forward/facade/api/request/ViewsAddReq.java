package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图新增请求")
public class ViewsAddReq extends BaseReq {

    @ApiModelProperty(value = "名称", required = true)
    @NotEmpty(message = "名称不能为空")
    private String name;

    @ApiModelProperty(value = "业务类型：10-业务需求，11-产品需求，12-项目，13-线下bug，14-线上bug", required = true)
    @NotNull(message = "业务类型不能为空")
    private Integer type;

    @ApiModelProperty(value = "负责人", required = true)
    @NotNull(message = "负责人不能为空")
    private PersonAddReq owner;

}
