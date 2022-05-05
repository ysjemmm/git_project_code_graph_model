package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;


/**
 * @author by YangXu
 * @date 2022/05/05 09:54
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目风险作废请求")
public class ProjectRiskInvalidReq extends BaseReq{

    @NotNull(message = "id不能为空")
    @ApiModelProperty("id")
    private Long id;
}
