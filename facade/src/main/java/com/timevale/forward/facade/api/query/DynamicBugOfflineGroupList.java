package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 线下Bug动态分组查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下Bug动态分组查询")
public class DynamicBugOfflineGroupList extends QueryBase {

    @ApiModelProperty("分组字段")
    private List<ViewsGroupQueryList> groupFields;

    @ApiModelProperty("筛选条件")
    private BugOfflineQueryList filters;

    @ApiModelProperty("分组筛选条件")
    private BugOfflineGroupList groupFilters;

    @ApiModelProperty("排序字段")
    private String orderField;
}
