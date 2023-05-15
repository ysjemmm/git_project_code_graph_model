package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;


/**
 * 业务状态变更操作人员日志
 *
 * @author by YangXu
 * @date 2023/05/15 15:18
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("业务状态变更操作人员日志")
public class BizStatusOperatorVO extends ToString {

    @ApiModelProperty("操作人")
    private String operatorName;

    @ApiModelProperty("操作时间")
    private Date operatorDate;
}