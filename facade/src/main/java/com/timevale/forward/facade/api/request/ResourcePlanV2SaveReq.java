package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 资源规划V2 - 批量保存请求
 *
 * @author kiro
 * @date 2026-02-24
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("资源规划V2-批量保存请求")
public class ResourcePlanV2SaveReq extends BaseReq {

    @NotNull(message = "业务域集ID不能为空")
    @ApiModelProperty(value = "业务域集ID", required = true)
    private Long bizDomainGroupId;

    @NotNull(message = "产品需求分组ID不能为空")
    @ApiModelProperty(value = "产品需求分组ID", required = true)
    private Long productDemandGroupId;

    @NotNull(message = "操作人ID不能为空")
    @ApiModelProperty(value = "操作人ID", required = true)
    private String operatorId;

    @NotNull(message = "操作人不能为空")
    @ApiModelProperty(value = "操作人", required = true)
    private String operator;

    @ApiModelProperty(value = "需求资源分配列表")
    private List<DemandOwnerTimeItem> items;

    @Data
    @ApiModel("单条资源分配")
    public static class DemandOwnerTimeItem {

        @NotNull(message = "产品需求ID不能为空")
        @ApiModelProperty(value = "产品需求ID", required = true)
        private Long productDemandId;

        @NotNull(message = "负责人ID不能为空")
        @ApiModelProperty(value = "负责人ID", required = true)
        private String ownerId;

        @NotNull(message = "负责人不能为空")
        @ApiModelProperty(value = "负责人姓名", required = true)
        private String owner;

        @NotNull(message = "资源类型不能为空")
        @ApiModelProperty(value = "资源类型: frontend/backend/qa/ued/product/ops/security", required = true)
        private String resourceType;

        @NotNull(message = "人天不能为空")
        @ApiModelProperty(value = "分配人天", required = true)
        private java.math.BigDecimal resourceTime;
    }
}
