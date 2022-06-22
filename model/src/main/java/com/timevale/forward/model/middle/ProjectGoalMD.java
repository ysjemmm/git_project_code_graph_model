package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.ProjectGoalStatusEnum;
import com.timevale.forward.model.enums.ProjectGoalTypeEnum;
import com.timevale.forward.model.enums.YesOrNoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @author jingchun
 * create on 2022/6/22
 */
@Getter
@Setter
public class ProjectGoalMD extends BaseMD {

    @FieldCompare(fieldName = "名称")
    private String name;

    @FieldCompare(fieldName = "目标性质", enumClass = ProjectGoalTypeEnum.class)
    private Integer type;

    @FieldCompare(fieldName = "是否主目标", enumClass = YesOrNoEnum.class)
    private Integer isMain;

    @FieldCompare(fieldName = "完成状态", enumClass = ProjectGoalStatusEnum.class)
    private Integer status;

    @FieldCompare(fieldName = "目标衡量标准")
    private String measurement;

    @FieldCompare(fieldName = "项目目标达标值")
    private BigDecimal reachValue;

    @FieldCompare(fieldName = "项目目标达成日期")
    private Date reachDate;

    @FieldCompare(fieldName = "完成情况")
    private String completeNote;

}
