package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author mayang
 * @date 2025-10-15 19:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求分组-资源规划-需求信息")
public class ResourcePlanProductDemandVO extends ToString {

    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("标签名称")
    private List<BizLabelSimpleVO> labelNames;

    @ApiModelProperty("需求详情")
    private ProductDemandResourceDetailVO productDemandDetail;

    @ApiModelProperty("资源类型 -> 需求负责人")
    private Map<String, List<ResourcePlanProductDemandOwnerVO>> owners;

    @Data
    @ApiModel("产品需求分组-资源规划-需求标准信息")
    public static class ProductDemandResourceDetailVO {
        @ApiModelProperty("产品需求id")
        private Long id;

        @ApiModelProperty("UED资源评估（人天）")
        private BigDecimal uedTime;

        @ApiModelProperty("后端资源评估（人天）")
        private BigDecimal backTime;

        @ApiModelProperty("前端资源评估（人天）")
        private BigDecimal frontTime;

        @ApiModelProperty("测试资源评估（人天）")
        private BigDecimal qaTime;

        @ApiModelProperty("产品资源评估（人天）")
        private BigDecimal productTime;

        @ApiModelProperty("运维迁移评估（人天）")
        private BigDecimal opsTime;

        @ApiModelProperty("安全资源评估（人天）")
        private BigDecimal securityTime;
    }
}
