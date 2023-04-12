package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@Setter
@ApiModel("任务完成请求")
public class TaskDoneReq extends ToString {

    @ApiModelProperty("任务id")
    @NotNull(message = "id必填")
    private Long id;

    @ApiModelProperty("实际结束时间")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd HH:mm")
    private Date actualEndDate;
}
