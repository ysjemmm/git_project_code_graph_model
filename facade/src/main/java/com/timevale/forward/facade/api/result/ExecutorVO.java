package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("执行人详情")
public class ExecutorVO extends ToString {

    @ApiModelProperty("花名拼音")
    private String userId;

    @ApiModelProperty("花名")
    private String userName;

    @ApiModelProperty("人员耗时")
    private BigDecimal useTime;


}
