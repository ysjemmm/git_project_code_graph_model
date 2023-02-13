package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.dal.entity.BaseDO;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;


/**
 * @author by YangXu
 * @date 2023/02/13 16:54
 */
@Setter
@Getter
public class ProjectBudgetMD extends BaseMD{

    @FieldCompare(fieldName = "成本类型")
    private String costType;

    @FieldCompare(fieldName = "预计成本金额（元）", scale = 2)
    private BigDecimal expectedAmount;

    @FieldCompare(fieldName = "实际已发生成本金额（元）", scale = 2)
    private BigDecimal costAmount;

    @FieldCompare(fieldName = "预计成本说明")
    private String expectedDesc;

    @FieldCompare(fieldName = "执行金额说明")
    private String costDesc;

    @FieldCompare(fieldName = "发生日期")
    private Date costDate;
}