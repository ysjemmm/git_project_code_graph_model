package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资源规划V2 - 查询返回
 *
 * @author kiro
 * @date 2026-02-24
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@ApiModel("资源规划V2-查询返回")
public class ResourcePlanV2VO extends ToString {

    @ApiModelProperty("产品需求分组ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long productDemandGroupId;

    @ApiModelProperty("需求列表（含每人资源分配）")
    private List<DemandWithOwnerTimes> demands;

    @Data
    @ApiModel("需求及其资源分配")
    public static class DemandWithOwnerTimes extends ToString {

        @ApiModelProperty("需求ID")
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @ApiModelProperty("需求名称")
        private String name;

        @ApiModelProperty("优先级:0(P0),10(P1),20(P2),30(P3)")
        private Integer priority;

        @ApiModelProperty("标签")
        private List<BizLabelSimpleVO> labelNames;

        @ApiModelProperty("资源分配明细（每人每类型一条）")
        private List<OwnerTimeItem> ownerTimes;
    }

    @Data
    @ApiModel("单人资源分配")
    public static class OwnerTimeItem extends ToString {

        @ApiModelProperty("记录ID")
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        @ApiModelProperty("需求ID")
        @JsonSerialize(using = ToStringSerializer.class)
        private Long productDemandId;

        @ApiModelProperty("负责人ID")
        private String ownerId;

        @ApiModelProperty("负责人姓名")
        private String owner;

        @ApiModelProperty("资源类型")
        private String resourceType;

        @ApiModelProperty("分配人天")
        private BigDecimal resourceTime;
    }
}
