package com.timevale.forward.facade.api.query;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("任务列表查询")
public class TaskQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("项目阶段")
    private List<Integer>stages;

    @ApiModelProperty("项目id")
    private List<Long> projectIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("状态:0待执行、10进行中、20已完成、-10已暂停、-20已作废")
    private List<Integer> status;

    @ApiModelProperty("执行人")
    private List<String> executorIds;

    @ApiModelProperty("创建人")
    private List<String> createManIds;

    @ApiModelProperty("计划开始时间左区间")
    private Date planStartDateLeft;

    @ApiModelProperty("计划开始时间右区间")
    private Date planStartDateRight;

    @ApiModelProperty("计划结束时间左区间")
    private Date planEndDateLeft;

    @ApiModelProperty("计划结束时间右区间")
    private Date planEndDateRight;

    @ApiModelProperty("实际开始时间左区间")
    private Date actualStartDateLeft;

    @ApiModelProperty("实际开始时间右区间")
    private Date actualStartDateRight;

    @ApiModelProperty("实际结束时间左区间")
    private Date actualEndDateLeft;

    @ApiModelProperty("实际结束时间右区间")
    private Date actualEndDateRight;

    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;
}
