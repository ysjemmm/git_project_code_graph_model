package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("标签类别列表查询")
public class LabelCategoryQueryList extends QueryBase {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("模块类型(10业务需求、11产品需求、12项目、13线下bug、14线上bug)")
    @NotNull(message = "模块类型不能为空")
    private List<Integer> types;

    @ApiModelProperty("业务域")
    @NotNull(message = "业务域不能为空")
    private List<Long> bizDomainIds;

}
