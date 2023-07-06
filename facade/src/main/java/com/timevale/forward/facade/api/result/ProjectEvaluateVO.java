package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/03/07 11:37
 */
@Getter
@Setter
@ApiModel("项目评价详情")
public class ProjectEvaluateVO extends ToString {

    @ApiModelProperty("项目评价总分")
    private BigDecimal scoresSum;

    @ApiModelProperty("SR建议评价等级")
    private Integer srEvaluateGrade;

    @ApiModelProperty("SR建议评价等级")
    private String srEvaluateGradeName;

    @ApiModelProperty("项目评价等级")
    private Integer projectEvaluateGrade;

    @ApiModelProperty("项目评价等级")
    private String projectEvaluateGradeName;

    @ApiModelProperty("项目实得积分")
    private BigDecimal projectPoint;

    @ApiModelProperty("项目评价详情项")
    private List<ProjectEvaluateItemVO> evaluateItemVOList;
}
