package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @auther: yuhua
 * @date: 2025/7/3 17:52
 * @description: 工时记录查询
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("工时记录查询")
public class WorkHoursRecordQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("项目ids")
    private List<Long> projectIds;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("工作项类别")
    private Integer workItemType;

    @ApiModelProperty("工作项id")
    private Long workItemId;

    @ApiModelProperty("创建人")
    private String createManId;

    @ApiModelProperty("创建时间左区间")
    private String startTime;

    @ApiModelProperty("创建时间右区间")
    private String endTime;
}
