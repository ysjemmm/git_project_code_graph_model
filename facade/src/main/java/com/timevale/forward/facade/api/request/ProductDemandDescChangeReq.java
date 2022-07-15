package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author jingchun
 * create on 2022/7/4
 */
@Getter
@Setter
@ApiModel("产品需求变更请求")
public class ProductDemandDescChangeReq extends ToString {

    @NotNull(message = "需求变更类型必填")
    @ApiModelProperty("变更类型:0需求调研不充分、1业务需求变更或新增、2业务需求理解偏差、3需求对现有业务流造成改动需调整方案、9其他")
    private Integer productDemandDescChangeType;

    @ApiModelProperty("其他变更类型原因")
    private String otherReason;

    @NotBlank(message = "变更后描述必填")
    @ApiModelProperty("变更后需求描述")
    private String changeDesc;

    @NotBlank(message = "变更事由必填")
    @ApiModelProperty("变更事由")
    private String reason;

    @NotBlank(message = "PO负责人必填")
    @ApiModelProperty("PO负责人Id")
    private String poId;

}
