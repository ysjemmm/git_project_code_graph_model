package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xiaoyun
 * @date 2022/8/29/029 17:10
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目更改状态请求")
public class ProjectUpdateStatusReq extends BaseReq {

    @NotNull(message = "id不能为空")
    @ApiModelProperty("id")
    private Long projectId;

    @ApiModelProperty("类型, -10:暂停,-20:作废")
    private Integer type;

    @ApiModelProperty("项目暂停原因")
    private String suspendReason;

    @ApiModelProperty("项目作废原因")
    private String invalidReason;
}
