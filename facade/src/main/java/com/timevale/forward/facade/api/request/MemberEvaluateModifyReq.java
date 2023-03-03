package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2022/06/24 09:52
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("成员评分修改")
public class MemberEvaluateModifyReq extends BaseReq{

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id必填")
    private Long projectId;

    @ApiModelProperty("用户id")
    @NotNull(message = "用户id必填")
    private Long userId;

    @ApiModelProperty("计划工作量")
    private BigDecimal planWorkLoad;

    @ApiModelProperty("实际工作量")
    private BigDecimal actualWorkLoad;

    @ApiModelProperty("评级")
    private Integer evaluateGrade;

    @ApiModelProperty("评价说明")
    private String evaluateExplain;

}
