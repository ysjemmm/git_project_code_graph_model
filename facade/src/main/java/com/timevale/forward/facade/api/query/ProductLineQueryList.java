package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品线查询")
public class ProductLineQueryList extends QueryBase {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("产品线负责人id")
    private List<String> ownerIds;

    @ApiModelProperty("线上bug负责人id")
    private List<String> bugOnlineOwnerIds;

}