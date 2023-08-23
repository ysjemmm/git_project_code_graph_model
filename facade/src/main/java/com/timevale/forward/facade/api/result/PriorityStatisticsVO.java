package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ClassName: PriorityStatisticsVO
 * @Author: shaoye
 * @Date: 2023-08-22 20:38
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("优先级统计信息")
public class PriorityStatisticsVO extends ToString {

    @ApiModelProperty("bug优先级：0-低，1-中，2-高，3-紧急")
    private Integer priority;

    @ApiModelProperty("数量")
    private Integer count;

}
