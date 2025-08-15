package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author qiyuan
 * @date 2025-07-16 17:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图分组查询")
public class ViewsGroupQuery extends ToString {
    @ApiModelProperty("业务类型：10-业务需求，11-产品需求，12-项目，13-线下bug，14-线上bug")
    private Integer type;

}
