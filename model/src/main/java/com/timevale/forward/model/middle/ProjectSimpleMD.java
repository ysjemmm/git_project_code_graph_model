package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectSimpleMD extends BaseMD{

    @FieldCompare(fieldName = "项目名称")
    private String name;

    @FieldCompare(fieldName = "项目计划开始时间")
    private Date planStartDate;

    @FieldCompare(fieldName = "项目计划结束时间")
    private Date planEndDate;

    @FieldCompare(fieldName = "项目经理")
    private String pm;

    @FieldCompare(fieldName = "项目描述")
    private String desc;

    @FieldCompare(fieldName = "项目等级",enumClass = ProjectLevelEnum.class)
    private Integer level;

    @FieldCompare(fieldName = "项目暂停原因")
    private String suspendReason;

    @FieldCompare(fieldName = "项目类型", enumClass = ProjectInnerTypeEnum.class)
    private Integer innerType;

    @FieldCompare(fieldName = "项目预计收益金额", scale = 2)
    private BigDecimal expectedIncome;
}