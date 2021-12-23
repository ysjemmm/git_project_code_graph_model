package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目-产品需求列表查询")
public class ProjectProductDemandQueryList extends QueryBase {

    @NotNull(message = "项目id不能为空")
    @ApiModelProperty("项目id")
    private Long projectId;

}
