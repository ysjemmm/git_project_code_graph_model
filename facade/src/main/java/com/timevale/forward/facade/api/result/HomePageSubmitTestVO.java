package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2022/01/26 17:51
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-风险预警-提测")
public class HomePageSubmitTestVO extends ToString {

    @ApiModelProperty("项目id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("提测单名称")
    private String testBillName;

    @ApiModelProperty("提测结果")
    private String testBillResult;
}
