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
@ApiModel("项目风险新增请求")
public class ProjectRiskAddReq extends BaseReq{

    @NotNull(message = "项目id不能为空")
    @ApiModelProperty("项目id")
    private Long projectId;

    @NotBlank(message = "标记风险名称不能为空")
    @ApiModelProperty("名称")
    private String name;

    @NotBlank(message = "风险说明不能为空")
    @ApiModelProperty("项目风险说明")
    private String explanation;
}
