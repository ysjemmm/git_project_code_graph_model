package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2022/01/27 15:51
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-近三周上线项目")
public class HomePageProjectOnlineLatelyVO extends ToString {

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目经理")
    private String pm;

    @ApiModelProperty("状态")
    private String status;

    @ApiModelProperty("项目计划上线时间")
    private Date planStartDate;
}
