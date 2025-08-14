package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图请求")
public class ViewsReq extends BaseReq {
    @ApiModelProperty("主键id")
    @NotNull(message = "主键id不能为空")
    private Long id;
}
