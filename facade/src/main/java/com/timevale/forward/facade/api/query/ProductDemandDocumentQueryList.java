package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ApiModel("产品需求列表查询接口")
public class ProductDemandDocumentQueryList extends QueryBase {

    @ApiModelProperty("项目id")
    private Long projectId;

}
