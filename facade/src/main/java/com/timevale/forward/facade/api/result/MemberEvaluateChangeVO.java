package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


/**
 * @author by YangXu
 * @date 2023/06/20 16:04
 */
@Getter
@Setter
@Accessors(chain = true)
@ApiModel("成员评分变更详情")
public class MemberEvaluateChangeVO extends ToString {

    @ApiModelProperty("用户名称")
    private String userName;

    @ApiModelProperty("计划工作量-调整前")
    private BigDecimal planWorkloadBefore;

    @ApiModelProperty("计划工作量-调整后")
    private BigDecimal planWorkloadAfter;

    @ApiModelProperty("是否纳入统计-描述")
    private String includeStatName;
}
