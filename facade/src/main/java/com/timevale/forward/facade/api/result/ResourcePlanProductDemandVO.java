package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
    private ResourcePlanProductDemandTimeVO productDemandDetail;

    @ApiModelProperty("资源类型 -> 需求负责人")
    private Map<String, List<ResourcePlanProductDemandOwnerVO>> owners;
}
