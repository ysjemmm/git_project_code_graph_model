package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品需求新增")
public class ProductDemandAddReq extends BaseReq {

    @ApiModelProperty("名称")
    @NotNull(message = "需求名称不能为空")
    @Length(max = 100, message = "需求名称长度不能超过100,请重新输入")
    private String name;

    @ApiModelProperty("优先级:0(P0),10(P1),20(P2),30(P3)")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @ApiModelProperty("产品线")
    @NotNull(message = "产品线不能为空")
    private Long productLineId;

    @ApiModelProperty("类型:0新增功能,1功能迭代,2体验优化,3技术需求,4安全需求")
    @NotNull(message = "需求类型不能为空")
    private List<Integer> types;

    @ApiModelProperty("需求负责人")
    @NotNull(message = "需求负责人不能为空")
    private PersonAddReq demandOwner;

    @ApiModelProperty("描述")
    @Length(max = 20000, message = "需求描述字数过大,请重新输入")
    private String desc;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipients;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;

    @ApiModelProperty("业务需求id")
    private List<Long> bizDemandIds;

    @ApiModelProperty("项目id")
    private Long projectId;

    @ApiModelProperty("埋点事件id")
    private List<Long> trackEventIds;

    @ApiModelProperty("客户需求id")
    private List<Long> customDemandIds;

    @ApiModelProperty("标签id")
    private List<Long> labelIds;

    @ApiModelProperty("预期排期时间")
    private Date expectScheduleTime;

    @ApiModelProperty("UED资源评估（人天）")
    private BigDecimal uedTime;

    @ApiModelProperty("后端资源评估（人天）")
    private BigDecimal backTime;

    @ApiModelProperty("前端资源评估（人天）")
    private BigDecimal frontTime;

    @ApiModelProperty("测试资源评估（人天）")
    private BigDecimal qaTime;

    @ApiModelProperty("总资源评估（人天）")
    private BigDecimal totalTime;

    @ApiModelProperty("功能迁移评估（人天）")
    private BigDecimal transferTime;

    @ApiModelProperty("产品资源评估（人天）")
    private BigDecimal productTime;

    @ApiModelProperty("运维迁移评估（人天）")
    private BigDecimal opsTime;

    @ApiModelProperty("安全资源评估（人天）")
    private BigDecimal securityTime;

    @ApiModelProperty("目标客户/用户/项目")
    private String targetCustomer;

    @ApiModelProperty("客户等级")
    private String customerGrade;

    @ApiModelProperty("产品需求负责人集合")
    private List<ProductDemandOwnerAddReq> productDemandOwners;
}
