package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Getter
@Setter
@ApiModel("项目新增")
public class ProjectAddReq extends ToString {

    @ApiModelProperty("项目名称")
    @NotNull(message = "项目名称不能为空")
    private String name;

    @ApiModelProperty("是否为客户开发项目：0否，1是")
    private Integer customerDev;

    @ApiModelProperty("优先级:0(P0),10(P1),20(P2)")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @NotNull(message = "项目类型必填")
    @ApiModelProperty(value = "项目类型: 0-产研项目; 1-内部项目", required = true)
    private Integer category = 0;

    @ApiModelProperty("产品线")
    @NotNull(message = "产品线不能为空")
    private List<Long> productLineIds;

    @ApiModelProperty("项目性质:0产品研发项目,1技术优化项目,2日常迭代，3自研项目，4ISV项目")
    @NotNull(message = "项目类型不能为空")
    private Integer type;

    @ApiModelProperty("项目经理")
    @NotNull(message = "项目经理不能为空")
    private PersonAddReq pm;

    @ApiModelProperty("产品经理")
    private List<PersonAddReq> pds;

    @ApiModelProperty("团队成员")
    private List<PersonAddReq> teamMembers;

    @ApiModelProperty("项目计划开始时间")
    @NotNull(message = "项目计划开始时间不能为空")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    @NotNull(message = "项目计划结束时间不能为空")
    private Date planEndDate;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("是否在发布平台发布：0否，1是")
    private Integer isPlatformPublish;

    @ApiModelProperty("是否有项目目标：0否，1是")
    private Integer isWithGoal;

    @ApiModelProperty("是否发送钉钉工时通知：0否，1是")
    private Integer workHoursNotify;

    @Valid
    @ApiModelProperty("项目目标列表")
    private List<ProjectGoalAddReq> projectGoals;

    @Valid
    @ApiModelProperty("项目预算列表")
    private List<ProjectBudgetSaveReq> projectBudgets;

    @ApiModelProperty(value = "项目等级：0普通 10重点 20S级别 30A级别 40B级别", required = true)
    @NotNull(message = "项目等级不能为空")
    private Integer level;

    @ApiModelProperty("产品技术资源评估（人天）")
    private BigDecimal resourceAssessment;

    @Digits(integer = 15, fraction = 2, message = "请输入15位以内整数，2位以内小数")
    @PositiveOrZero(message = "预计收益金额不可为负数")
    @ApiModelProperty("项目预计收益金额")
    private BigDecimal expectedIncome;

    @NotNull(message = "内部项目类型必填")
    @ApiModelProperty(value = "内部项目类型: 0空, 1战略项目, 2LTC项目, 3PBG项目, 4CBG项目, 5管理后台项目", required = true)
    private Integer innerType = 0;

    @ApiModelProperty("标签id")
    private List<Long> labelIds;

    @ApiModelProperty("是否需要验收：0否，1是")
    @NotNull(message = "是否需要验收不能为空")
    private Integer isAcceptance;

    @ApiModelProperty("父级项目id")
    private Long parentId;

    @ApiModelProperty("项目类型，0-空，1-PBG项目/基线项目，2-PBG项目/1-N客开项目，3-职能后台项目/流程IT中心项目")
    @NotNull(message = "项目类型必填")
    private Integer kind;

    @ApiModelProperty("sr")
    @NotNull(message = "sr不能为空")
    private PersonAddReq sr;

    @ApiModelProperty("负责人")
    @NotNull(message = "项目负责人不能为空")
    private PersonAddReq principal;

    @ApiModelProperty("1-N产研团队负责人")
    @NotNull(message = "1-N产研团队负责人不能为空")
    private PersonAddReq otnPrincipal;

    @ApiModelProperty("来源id")
    private String sourceId;

    @ApiModelProperty("业务需求列表")
    private List<Long> bizDemandIds;

    @ApiModelProperty("节点")
    @NotNull(message = "项目节点不能为空")
    private List<ProjectNodeAddReq> projectNodes;

}
