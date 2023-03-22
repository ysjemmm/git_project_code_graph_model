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
@ApiModel("成员工作量修改请求")
public class MemberWorkloadModifyReq extends ToString {

    @ApiModelProperty("用户id")
    @NotNull(message = "用户id必填")
    private String userId;

    @ApiModelProperty("计划工作量")
    private BigDecimal planWorkload;

    @ApiModelProperty("是否纳入积分统计")
    private Boolean includeStat;
}
