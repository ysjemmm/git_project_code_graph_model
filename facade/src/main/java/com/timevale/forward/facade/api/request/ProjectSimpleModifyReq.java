package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/1
 */
@Getter
@Setter
@ApiModel("项目简单修改")
public class ProjectSimpleModifyReq extends ToString {

    @NotNull(message = "项目id必填")
    @ApiModelProperty(value = "项目id", required = true)
    private Long projectId;

    @NotNull
    @ApiModelProperty("内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目")
    private Integer innerType;

    @ApiModelProperty("项目等级：0普通 10重点 20S级别 30A级别 40B级别")
    private Integer level;

    @ApiModelProperty("项目经理")
    private PersonAddReq pm;

    @ApiModelProperty("项目计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("描述")
    private String desc;

    @Digits(integer = 15, fraction = 2, message = "请输入15位以内整数，2位以内小数")
    @PositiveOrZero(message = "预计收益金额不可为负数")
    @ApiModelProperty("项目预计收益金额")
    private BigDecimal expectedIncome;

    @ApiModelProperty("团队成员")
    private List<PersonAddReq> teamMembers;

}
