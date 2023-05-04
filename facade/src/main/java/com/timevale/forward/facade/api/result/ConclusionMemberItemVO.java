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
@ApiModel("结项信息成员评分详情")
public class ConclusionMemberItemVO extends ToString {
    @ApiModelProperty("用户名称")
    private String userName;

    @ApiModelProperty("计划工作量")
    private BigDecimal planWorkload;

    @ApiModelProperty("再分配-工作量")
    private BigDecimal actualWorkload;

    @ApiModelProperty("评级描述")
    private String evaluateGradeName;

    @ApiModelProperty("评价说明")
    private String evaluateExplain;
}
