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
@ApiModel("任务-产品需求查询")
public class TaskLinkProductDemandQueryList extends QueryBase {

    @ApiModelProperty("任务名称")
    private String name;

    @ApiModelProperty("任务id:编辑时填写")
    private Long id;

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id不能为空")
    private Long projectId;
}
