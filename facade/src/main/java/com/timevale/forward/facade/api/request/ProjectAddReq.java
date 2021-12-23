package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目新增")
public class ProjectAddReq extends BaseReq {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private Integer priority;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    private Integer type;

    @ApiModelProperty("项目经理")
    @NotNull(message = "项目经理不能为空")
    private PersonAddReq pm;

    @ApiModelProperty("产品经理")
    private List<PersonAddReq> pds;

    @ApiModelProperty("团队成员")
    private List<PersonAddReq> teamMembers;

    @ApiModelProperty("项目计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("描述")
    private String desc;

}
