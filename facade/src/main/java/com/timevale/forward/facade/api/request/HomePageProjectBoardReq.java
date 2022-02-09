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
@ApiModel("首页-项目工时看板查询")
public class HomePageProjectBoardReq extends BaseReq {

    @ApiModelProperty("部门id")
    private List<Long> deptIds;

    @ApiModelProperty("团队成员:花名id")
    private List<String> teamMembers;

    @ApiModelProperty("开始时间")
    private Date startDate;

    @ApiModelProperty("结束时间")
    private Date endDate;

    @ApiModelProperty("用户类型")
    @NotNull
    private String userType;
}
