package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-待办卡片")
public class HomePageTodoCardVO extends ToString {

    @ApiModelProperty("项目总数")
    private Integer projectCount;

    @ApiModelProperty("任务数量")
    private Integer taskCount;

    @ApiModelProperty("业务需求数量")
    private Integer bizDemandCount;

}
