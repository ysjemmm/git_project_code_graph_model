package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @author xingyun
 * @date 2026-01-09
 * @description: 产品需求批量查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求批量查询请求")
public class ProductDemandBatchQueryReq extends BaseReq {

    @ApiModelProperty(value = "需求ID集合", required = true)
    @NotEmpty(message = "需求ID集合不能为空")
    private List<Long> ids;
}