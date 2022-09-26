package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-任务工时看板节假日查询")
public class HomePageHolidayReq extends BaseReq {

    @ApiModelProperty("开始时间")
    @NotNull(message = "开始时间不能为空")
    private Date startDate;

    @ApiModelProperty("结束时间")
    @NotNull(message = "结束时间不能为空")
    private Date endDate;
}
