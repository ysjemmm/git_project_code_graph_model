package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/8/13 15:02
 * @description: 动态分组查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("动态分组查询")
public class DynamicProductDemandGroupList extends QueryBase {

    @ApiModelProperty("分组字段")
    private List<String> groupFields;

    @ApiModelProperty("筛选条件")
    private ProductDemandQueryList filters;

    @ApiModelProperty("父筛选条件")
    private ProductDemandGroupList parentConditions;

    @ApiModelProperty("排序字段")
    private String orderField;
}
