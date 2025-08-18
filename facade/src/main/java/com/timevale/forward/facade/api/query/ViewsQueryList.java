package com.timevale.forward.facade.api.query;

import com.timevale.forward.facade.api.request.BaseReq;
import com.timevale.mandarin.common.query.QueryBase;
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
@ApiModel("视图查询")
public class ViewsQueryList extends BaseReq {
    @ApiModelProperty("所属类型：0-自己创建的，1-他人分享的")
    private Integer ownerType;

    @ApiModelProperty("业务类型：10-业务需求，11-产品需求，12-项目，13-线下bug，14-线上bug")
    private Integer type;

    @ApiModelProperty("下拉列表属于设置为true")
    private Boolean selectForOptions;
}
