package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量移动产品需求到目标分组请求
 * @author kiro
 * @date 2026/03/02
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("批量移动产品需求到目标分组请求")
public class ProductDemandGroupBatchMoveReq extends BaseReq {

    @ApiModelProperty(value = "产品需求ID列表（productDemandId）", required = true)
    @NotEmpty(message = "产品需求ID列表不能为空")
    private List<Long> productDemandIds;

    @ApiModelProperty(value = "目标分组ID", required = true)
    @NotNull(message = "目标分组ID不能为空")
    private Long targetGroupId;

    @ApiModelProperty(value = "业务域集ID", required = true)
    @NotNull(message = "业务域集ID不能为空")
    private Long bizDomainGroupId;
}
