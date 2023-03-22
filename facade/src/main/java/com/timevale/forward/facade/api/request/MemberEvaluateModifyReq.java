package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * @author by YangXu
 * @date 2022/06/24 09:52
 */
@Getter
@Setter
@ApiModel("成员评价请求")
public class MemberEvaluateModifyReq extends ToString {

    @ApiModelProperty("项目id")
    @NotNull(message = "项目id必填")
    private Long projectId;

    @ApiModelProperty("用户id")
    @NotNull(message = "用户id必填")
    private String userId;

    @ApiModelProperty("实际工作量")
    private BigDecimal actualWorkload;

    @ApiModelProperty("评级")
    private Integer evaluateGrade;

    @ApiModelProperty("评价说明")
    private String evaluateExplain;

    @ApiModelProperty("是否纳入积分统计")
    private Boolean includeStat;

}
