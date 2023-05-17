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
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("成员评分详情")
public class MemberEvaluateVO extends ToString {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("用户id")
    private String userId;

    @ApiModelProperty("用户名称")
    private String userName;

    @ApiModelProperty("计划工作量")
    private BigDecimal planWorkload;

    @ApiModelProperty("实际工作量")
    private BigDecimal actualWorkload;

    @ApiModelProperty("评级")
    private Integer evaluateGrade;

    @ApiModelProperty("评级描述")
    private String evaluateGradeName;

    @ApiModelProperty("评价说明")
    private String evaluateExplain;

    @ApiModelProperty("是否纳入统计")
    private Boolean includeStat;

    @ApiModelProperty("实得积分")
    private BigDecimal personalPoints;
}
