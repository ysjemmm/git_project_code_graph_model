package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
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
@ApiModel("项目详情")
public class ProjectDetailVO extends ToString {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("是否为客户开发项目：0否，1是")
    private Integer customerDev;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废")
    private Integer status;

    @ApiModelProperty("项目状态")
    private String statusName;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private Integer priority;

    @ApiModelProperty("优先级")
    private String priorityName;

    @ApiModelProperty("产品线")
    private List<ProductLineVO> productLineVO;

    @ApiModelProperty("项目类型:0产品研发项目,1技术优化项目,2日常迭代")
    private Integer type;

    @ApiModelProperty("项目类型")
    private String typeName;

    @ApiModelProperty("项目经理id")
    private String pmId;

    @ApiModelProperty("项目经理")
    private String pmName;

    @ApiModelProperty("产品经理")
    private List<PersonVO> pd;

    @ApiModelProperty("团队成员")
    private List<PersonVO> teamMember;

    @ApiModelProperty("项目计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("项目实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("项目实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("产品需求")
    private List<ProductDemandVO> productDemandVO;
    
    @ApiModelProperty("节点")
    private List<ProjectNodeVO> projectNodes;

    @ApiModelProperty("当前时间")
    private Date currentDate;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建人id")
    private String createManId;

    @ApiModelProperty("节点状态")
    private Integer nodeStatus;

    @ApiModelProperty("节点状态名称")
    private String nodeStatusName;

    @ApiModelProperty("项目流程id")
    private Long projectFlowId;

    @ApiModelProperty("是否在发布平台发布")
    private Integer isPlatformPublish;

    @ApiModelProperty("发布正式流程id")
    private Long publishFlowId;

    @ApiModelProperty("发布正式流程状态")
    private Long publishFlowStatus;
}
