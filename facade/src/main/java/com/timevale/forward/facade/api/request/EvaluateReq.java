package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * @author by YangXu
 * @date 2022/06/24 09:52
 */
@Getter
@Setter
@ApiModel("项目评价修改请求")
public class EvaluateReq extends ToString {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id必填")
    private Long projectId;

    @ApiModelProperty("维度id")
    @NotNull(message = "维度id必填")
    private Long evaluateDimensionId;

    @ApiModelProperty("评分")
    private Integer scores;

    @ApiModelProperty("评分描述")
    private String scoresDesc;

}
