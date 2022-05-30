package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;


/**
 * @author by YangXu
 * @date 2022/05/25 16:50
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目维度看板-线下bug趋势图")
public class BugOfflineTrendVO extends ToString {

    @ApiModelProperty("日期")
    private Date date;

    @ApiModelProperty("累积创建bug")
    private Integer createdBug;

    @ApiModelProperty("累积解决bug")
    private Integer solvedBug;
}
