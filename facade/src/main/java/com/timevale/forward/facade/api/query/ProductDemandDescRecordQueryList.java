package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * @author jingchun
 * create on 2022/7/1
 */
@Getter
@Setter
@ApiModel("需求描述历史版本列表查询入参")
public class ProductDemandDescRecordQueryList extends QueryBase {

    @ApiModelProperty("产品需求id")
    private Long productDemandId;

}
