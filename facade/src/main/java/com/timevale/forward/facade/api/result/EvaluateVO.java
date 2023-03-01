package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


/**
 * @author by YangXu
 * @date 2023/02/06 15:31
 */
@Getter
@Setter
@ApiModel("项目结项申请单详情")
public class EvaluateVO extends ToString {

    @ApiModelProperty("评价id")
    private Long id;

    @ApiModelProperty("项目维度名称")
    private String dimensionName;

    @ApiModelProperty("评分")
    private Integer scores;

    @ApiModelProperty("评分描述")
    private Integer scoresDesc;

}
