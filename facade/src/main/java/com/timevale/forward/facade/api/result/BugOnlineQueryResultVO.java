package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @ClassName: BugOnlineQueryResultVO
 * @Author: shaoye
 * @Date: 2023-08-22 20:35
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("线上BUG列表数据")
public class BugOnlineQueryResultVO<T>  extends ToString {

    @ApiModelProperty("优先级统计列表")
    private List<PriorityStatisticsVO> priorityStatisticsVOList;

    @ApiModelProperty("分页查询列表")
    private PageQueryResult<T> pageQueryResult;

}
