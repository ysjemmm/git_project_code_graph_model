package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@ApiModel("工作日工时查询")
public class ElapsedTimeQueryReq extends BaseReq {

    @ApiModelProperty("开始时间")
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date startTime;

    @ApiModelProperty("结束时间")
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date endTime;
}
