package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目-发布计划关联")
public class ProjectPublishPlanLinkReq extends BaseReq {

    @ApiModelProperty("项目id不能为空")
    @NotNull(message = "项目id不能为空")
    private Long projectId;

    @NotNull(message = "发布计划id不能为空")
    @ApiModelProperty("发布计划id")
    private List<Long> publishPlanId;

    @ApiModelProperty("0:关联,1:取消")
    private Integer type;
}
