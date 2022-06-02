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
@ApiModel("项目列表查询")
public class ProjectQueryList extends QueryBase {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("是否为客户开发项目：0否，1是")
    private Integer customerDev;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private List<Integer> priorities;

    @ApiModelProperty("业务域")
    private List<Long> bizDomainIds;

    @ApiModelProperty("产品线")
    private List<Long> productLineIds;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    private List<Integer> types;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废")
    private List<Integer> status;

    @ApiModelProperty("项目经理")
    private List<String> pms;

    @ApiModelProperty("产品经理")
    private List<String> pds;

    @ApiModelProperty("团队成员")
    private List<String> teamMembers;

    @ApiModelProperty("项目节点状态：0 开始规划,10 需求内审,20 需求串讲,30 技术详设评审,40 开发开始,50 编写测试用例,60 用例评审,70 提测,80 测试开始,90 发布模拟,100 发布正式")
    private List<Long> nodeStatusList;

    @ApiModelProperty("项目计划开始时间左区间")
    private Date planStartDateLeft;

    @ApiModelProperty("项目计划开始时间右区间")
    private Date planStartDateRight;

    @ApiModelProperty("项目计划结束时间左区间")
    private Date planEndDateLeft;

    @ApiModelProperty("项目计划结束时间右区间")
    private Date planEndDateRight;

    @ApiModelProperty("项目实际开始时间左区间")
    private Date actualStartDateLeft;

    @ApiModelProperty("项目实际开始时间右区间")
    private Date actualStartDateRight;

    @ApiModelProperty("项目实际结束时间左区间")
    private Date actualEndDateLeft;

    @ApiModelProperty("项目实际结束时间右区间")
    private Date actualEndDateRight;

    @ApiModelProperty("创建时间左区间")
    private Date createDateLeft;

    @ApiModelProperty("创建时间右区间")
    private Date createDateRight;

    @ApiModelProperty("CURRENT_USER:我的,FOLLOWER:我下属的,TEAM:我团队的,DEPARTMENT:我部门的,COPIER:抄送我的,RECEIVE:我接收的,ALL:全部")
    private String ascription;

    @ApiModelProperty("提测打回次数判断类型:0=,1>,2>=,3<,4<=")
    private Integer returnCountType;

    @ApiModelProperty("提测打回次数")
    private Integer returnCount;

    @ApiModelProperty("提测是否延期")
    private Boolean isDelay;

    @ApiModelProperty("提测时间左区间")
    private Date actualTestDateLeft;

    @ApiModelProperty("提测时间右区间")
    private Date actualTestDateRight;

}
