package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求详情")
public class ProductDemandDetailVO extends ToString {

    @ApiModelProperty("名称")
    private String name;

    @ApiModelProperty("优先级:0(P0),10(P1),20(P2),30(P3)")
    private Integer priority;

    @ApiModelProperty("优先级")
    private String priorityName;

    @ApiModelProperty("产品线")
    private ProductLineVO productLineVO;

    @ApiModelProperty("类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求")
    private String type;

    @ApiModelProperty("类型")
    private List<String> typeName;

    @ApiModelProperty("类型")
    private List<Integer> types;

    @ApiModelProperty("状态:0待排期,10已列入项目,20项目进行中,30已完成上线,-10已暂停,-20已作废")
    private Integer status;

    @ApiModelProperty("状态")
    private String statusName;

    @ApiModelProperty("负责人")
    private String owner;

    @ApiModelProperty("负责人id")
    private String ownerId;

    @ApiModelProperty("描述")
    private String desc;

    @ApiModelProperty("变更后描述")
    private String changeDesc;

    @ApiModelProperty("产品需求变更(成功)次数")
    private Integer descChangeTimes;

    @ApiModelProperty("抄送人")
    private List<PersonVO> recipients;

    @ApiModelProperty("附件")
    private List<FileVO> files;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("预期排期时间")
    private Date expectScheduleTime;

    @ApiModelProperty("标签集合")
    private List<LabelDetailVO> labelDetailVOS;

    @ApiModelProperty("UED资源评估（人天）")
    private BigDecimal uedTime;

    @ApiModelProperty("后端资源评估（人天）")
    private BigDecimal backTime;

    @ApiModelProperty("前端资源评估（人天）")
    private BigDecimal frontTime;

    @ApiModelProperty("测试资源评估（人天）")
    private BigDecimal qaTime;

    @ApiModelProperty("功能迁移评估（人天）")
    private BigDecimal transferTime;

    @ApiModelProperty("产品资源评估（人天）")
    private BigDecimal productTime;

    @ApiModelProperty("运维资源评估（人天）")
    private BigDecimal opsTime;

    @ApiModelProperty("安全迁移评估（人天）")
    private BigDecimal securityTime;

    @ApiModelProperty("总资源评估（人天）")
    private BigDecimal totalTime;

    @ApiModelProperty("目标客户/用户/项目")
    private String targetCustomer;

    @ApiModelProperty("客户等级")
    private String customerGrade;

    @ApiModelProperty("资源评估信息")
    private List<SimpleResourcePlanItemVO> resourcePlans;


    @ApiModelProperty("故事点（SP）")
    private Integer storyPoint;

}
