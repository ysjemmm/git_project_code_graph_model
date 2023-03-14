package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("项目列表")
public class ProjectVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("是否为客户开发项目：0否，1是")
    private Integer customerDev;

    @ApiModelProperty("优先级:0(P0),1(P1),2(P2)")
    private Integer priority;

    @ApiModelProperty("项目性质:0产品研发项目,1技术优化项目,2日常迭代，3自研项目，4ISV项目")
    private Integer type;

    @ApiModelProperty("项目状态：0待启动,5启动中，10规划中,15执行中,20研发中,25收尾中,30测试中,35运营中,40已发布,45已完成,50已结项，-10已暂停,-20已中止")
    private Integer status;

    @ApiModelProperty("项目计划开始时间")
    private Date planStartDate;

    @ApiModelProperty("项目计划结束时间")
    private Date planEndDate;

    @ApiModelProperty("项目实际开始时间")
    private Date actualStartDate;

    @ApiModelProperty("项目实际结束时间")
    private Date actualEndDate;

    @ApiModelProperty("创建人id")
    private String createManId;
    
    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("项目经理Id")
    private String pmId;

    @ApiModelProperty("项目经理")
    private String pmName;

    @ApiModelProperty("产品经理Id")
    private List<String> pdId;

    @ApiModelProperty("产品经理")
    private String pdName;

    @ApiModelProperty("业务域")
    private String bizDomainName;

    @ApiModelProperty("产品线")
    private String productLineName;

    @ApiModelProperty("团队成员")
    private String teamMember;

    @ApiModelProperty("优先级")
    private String priorityName;

    @ApiModelProperty("项目类型")
    private String typeName;

    @ApiModelProperty("项目状态")
    private String statusName;

    @ApiModelProperty("是否需要预警")
    private Boolean containRisk;

    @ApiModelProperty("节点状态")
    private Integer nodeStatus;

    @ApiModelProperty("节点状态名称")
    private String nodeStatusName;

    @ApiModelProperty("节点计划时间")
    private Date nodePlanDate;

    @ApiModelProperty("项目等级")
    private Integer level;

    @ApiModelProperty("类别")
    private Integer category;

    @ApiModelProperty("项目等级-描述")
    private String levelName;

    @ApiModelProperty("提测打回次数")
    private Integer returnCount;

    @ApiModelProperty("子项目节点数")
    private Long childrenCount;

    @ApiModelProperty("提测是否延期")
    private Boolean isDelay;

    @ApiModelProperty("提测实际时间")
    private Date actualTestDate;

    @ApiModelProperty("内部项目类型")
    private Integer innerType;

    @ApiModelProperty("内部项目类型名称")
    private String innerTypeName;

    @ApiModelProperty("节点深度(相对)")
    private Integer nodeDepth;

    @ApiModelProperty("项目类型-描述")
    private String kindName;

    @ApiModelProperty("SR")
    private String sr;

    @ApiModelProperty("结项时间")
    private Date conclusionDate;

    @ApiModelProperty("是否存在审批中的结项流程")
    private Boolean conclusionAuditing;

    @ApiModelProperty("标签名称")
    private List<BizLabelSimpleVO> labelNames;
}
