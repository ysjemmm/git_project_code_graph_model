package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


/**
 * @author by YangXu
 * @date 2022/04/24 16:48
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目风险修改请求")
public class ProjectRiskModifyReq extends BaseReq{

    @NotNull(message = "id不能为空")
    @ApiModelProperty("id")
    private Long id;

    @NotBlank(message = "标记风险名称不能为空")
    @ApiModelProperty("名称")
    private String name;

    @NotNull(message = "风险状态不能为空")
    @ApiModelProperty("状态: -1 作废 0 待处理, 1 已处理")
    private Integer state;
}
