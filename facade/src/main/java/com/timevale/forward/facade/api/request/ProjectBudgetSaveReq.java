package com.timevale.forward.facade.api.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Digits;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 */
@Getter
@Setter
@ApiModel("项目预算添加接口")
public class ProjectBudgetSaveReq extends ToString {

    @ApiModelProperty("预算id")
    private Long id;

    @ApiModelProperty("项目id")
    private Long projectId;

    @Length(max = 20, message = "成本类型不可超过20字")
    @ApiModelProperty("成本类型")
    private String costType;


    @Digits(integer = 15, fraction = 2, message = "请输入15位以内整数，2位以内小数")
    @PositiveOrZero(message = "预计成本金额不可为负数")
    @ApiModelProperty("预计成本金额（元）")
    private BigDecimal expectedAmount;

    @Digits(integer = 15, fraction = 2, message = "请输入15位以内整数，2位以内小数")
    @PositiveOrZero(message = "实际已发生金额不可为负数")
    @ApiModelProperty("实际已发生成本金额（元）")
    private BigDecimal costAmount;

    @Length(max = 100, message = "预计成本说明不可超过100字")
    @ApiModelProperty("预计成本说明")
    private String expectedDesc;

    @Length(max = 100, message = "执行金额说明不可超过100字")
    @ApiModelProperty("执行金额说明")
    private String costDesc;

    @ApiModelProperty("发生日期")
    @JsonFormat(timezone="GMT+8", pattern="yyyy-MM-dd")
    private Date costDate;

}
