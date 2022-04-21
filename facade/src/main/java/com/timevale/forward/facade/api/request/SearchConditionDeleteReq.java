package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2022/04/21 14:17
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("查询条件删除请求")
public class SearchConditionDeleteReq extends BaseReq{

    @ApiModelProperty("id")
    @NotNull(message = "id不能为空")
    private Long id;
}
