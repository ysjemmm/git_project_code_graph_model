package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author qiyuan
 * create on 2025/7/14
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("视图保存请求")
public class ViewsSaveReq extends ViewsReq {
    @ApiModelProperty("分组字段")
    private List<ViewsGroupFieldReq> groupFields;

    @ApiModelProperty("过滤条件")
    private ViewsFilterReq filterList;
}