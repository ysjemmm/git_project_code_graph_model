package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("事件属性新增")
public class TrackPropAddReq extends BaseReq {

    @ApiModelProperty("中文名称")
    private String cnName;

    @ApiModelProperty("英文名称")
    private String egName;

    @ApiModelProperty("属性类型:0新增属性,1已有属性,2默认属性")
    private Integer type;

    @ApiModelProperty("数据类型")
    private String dataType;

    @ApiModelProperty("属性状态:-1已撤回,0审核中,1审核通过,2审核不通过")
    @NotNull(message = "属性状态不能为空")
    private Integer status;
}
