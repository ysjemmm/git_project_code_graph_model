package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.result.ToString;
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
@ApiModel("查询类别下的标签")
public class LabelInCategoryQueryList extends ToString {

    @ApiModelProperty("模块类型(10业务需求、11产品需求、12项目、13线下bug、14线上bug)")
    @NotNull(message = "模块类型不能为空")
    private List<Integer> types;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("是否需要标记权限")
    @NotNull(message = "是否需要标记权限不能为空")
    private Boolean auth;

}
