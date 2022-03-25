package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @Date 2022/3/17 15:16
 * @Author 望轩
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("bug状态经办人及其耗时明细")
public class BugStatusOperatorVO extends ToString {
    @ApiModelProperty("经办人")
    private String operator;

    @ApiModelProperty("经办人id")
    private String operatorId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("bug变更的id")
    private Long bugLogId;
}