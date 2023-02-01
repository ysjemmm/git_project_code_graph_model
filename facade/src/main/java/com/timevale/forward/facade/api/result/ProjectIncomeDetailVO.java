package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
@ApiModel("项目收益情况详情")
public class ProjectIncomeDetailVO extends ToString {

    @ApiModelProperty("项目预计收益金额")
    private BigDecimal expectedIncome;

    @ApiModelProperty("项目收益进度")
    private BigDecimal progress;

    private List<ProjectIncomeVO> incomes;

}
