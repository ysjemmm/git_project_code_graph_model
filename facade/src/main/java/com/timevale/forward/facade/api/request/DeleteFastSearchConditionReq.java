package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @Description: 删除快捷搜索条件请求对象
 * @ClassName: DeleteFastSearchConditionReq
 * @Author: shaoye
 * @Date: 2023-08-26 11:26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-删除快捷搜索条件请求对象")
public class DeleteFastSearchConditionReq extends BaseReq {

    @ApiModelProperty(value = "类型：1-部门，2-成员", required = true)
    @NotNull(message = "类型不能为空")
    private Integer type;

    @ApiModelProperty(value = "ID", required = true)
    @NotEmpty(message = "ID不能为空")
    private String id;

}
