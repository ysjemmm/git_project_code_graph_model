package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;


/**
 * @author by YangXu
 * @date 2023/03/07 11:37
 */
@Getter
@Setter
@ApiModel("项目评价详情项")
public class ProjectEvaluateItemVO extends ToString {

    @ApiModelProperty("项目考核维度id")
    private Long evaluateDimensionId;

    @ApiModelProperty("项目考核维度名称")
    private String dimensionName;

    @ApiModelProperty("评分")
    private Integer scores;

    @ApiModelProperty("评分描述")
    private String scoresDesc;

    @ApiModelProperty("PMO评分")
    private Integer pmoScores;

    @ApiModelProperty("PMO评分描述")
    private String pmoScoresDesc;
}
