package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;


/**
 * @author by YangXu
 * @date 2023/02/03 10:33
 */
@Getter
@Setter
@ApiModel("内部项目新增")
public class ProjectInnerAddReq extends ToString {

    @NotBlank(message = "项目名称不能为空")
    @ApiModelProperty("项目名称")
    private String name;

    @NotNull(message = "内部项目类型必填")
    @ApiModelProperty(value = "内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目", required = true)
    private Integer innerType = 0;

    @ApiModelProperty(value = "项目等级：0普通 10重点 20S级别 30A级别 40B级别", required = true)
    @NotNull(message = "项目等级不能为空")
    private Integer level;

    @Valid
    @ApiModelProperty("项目经理")
    @NotNull(message = "项目经理不能为空")
    private PersonAddReq pm;

    @Valid
    @NotEmpty(message = "团队成员不能为空")
    @ApiModelProperty("团队成员")
    private List<PersonAddReq> teamMembers;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("项目计划开始时间")
    @NotNull(message = "项目计划开始时间不能为空")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    @NotNull(message = "项目计划结束时间不能为空")
    private Date planEndDate;

    @Valid
    @ApiModelProperty("项目目标列表")
    private List<ProjectGoalAddReq> projectGoals;

    @Valid
    @ApiModelProperty("项目预算列表")
    private List<ProjectBudgetSaveReq> projectBudgets;

    @Digits(integer = 15, fraction = 2, message = "请输入15位以内整数，2位以内小数")
    @PositiveOrZero(message = "预计收益金额不可为负数")
    @ApiModelProperty("项目预计收益金额")
    private BigDecimal expectedIncome;
}
