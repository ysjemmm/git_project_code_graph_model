package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @Date 2021/12/14 18:21
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求")
public class ProductDemandVO extends ToString {

    @ApiModelProperty("产品需求id")
    private Long id;

    @ApiModelProperty("产品需求主题")
    private String name;

    @ApiModelProperty("优先级")
    private Integer priority;

    @ApiModelProperty("业务域")
    private Long bizDomainId;

    @ApiModelProperty("需求状态")
    private Integer status;

    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("产品需求负责人")
    private String owner;
}
