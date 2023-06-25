package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("首页-项目工时看板查询")
public class HomePageProjectBoardReq extends HomePageBaseReq {

    @ApiModelProperty("部门id")
    private List<String> deptIds;

    @ApiModelProperty("团队成员:花名id")
    private Set<String> teamMembers;

    @ApiModelProperty("开始时间")
    private Date startDate;

    @ApiModelProperty("结束时间")
    private Date endDate;
}
