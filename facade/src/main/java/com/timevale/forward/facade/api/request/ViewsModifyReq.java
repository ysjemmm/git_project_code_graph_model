package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图修改请求")
public class ViewsModifyReq extends ViewsReq {
    @ApiModelProperty(value = "名称", required = true)
    @NotEmpty(message = "名称不能为空")
    @Size(max = 64,message = "视图名称长度不能超过64字符")
    private String name;
}
