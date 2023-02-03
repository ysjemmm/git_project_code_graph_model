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
@ApiModel("首页-任务工时看板查询")
public class HomePageTaskBoardReq extends BaseReq {

    @ApiModelProperty("tab类型：0为个人，1为团队")
    Integer tabType;

    @ApiModelProperty("部门id")
    private List<Long> deptIds;

    @ApiModelProperty("团队成员:花名id")
    private List<String> teamMembers;

    @ApiModelProperty("开始时间")
    @NotNull(message = "开始时间不能为空")
    private Date startDate;

    @ApiModelProperty("结束时间")
    @NotNull(message = "结束时间不能为空")
    private Date endDate;
}
