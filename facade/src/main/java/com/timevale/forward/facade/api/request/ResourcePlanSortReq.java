package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 资源规划需求排序请求（用需求id，后端内部转分组项id）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("资源规划需求排序请求")
public class ResourcePlanSortReq extends BaseReq {

    @ApiModelProperty(value = "业务域集id", required = true)
    @NotNull(message = "业务域集id不能为空")
    private Long bizDomainGroupId;

    @ApiModelProperty(value = "产品需求分组id", required = true)
    @NotNull(message = "产品需求分组id不能为空")
    private Long productDemandGroupId;

    @ApiModelProperty(value = "被拖拽的产品需求id", required = true)
    @NotNull(message = "产品需求id不能为空")
    private Long productDemandId;

    @ApiModelProperty("前一个产品需求id（null表示拖到最前面）")
    private Long prevDemandId;

    @ApiModelProperty("后一个产品需求id（null表示拖到最后面）")
    private Long nextDemandId;
}
