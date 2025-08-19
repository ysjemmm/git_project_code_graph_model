package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 15:04
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("业务需求新增")
public class BizDemandAddReq extends BaseReq {

    @ApiModelProperty("需求主题")
    @NotBlank(message = "需求主题不能为空")
    private String name;

    @ApiModelProperty("需求部门id")
    @NotNull(message = "需求部门不能为空")
    private Long deptId;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    @NotNull(message = "必须选择优先级")
    private Integer priority;

    @ApiModelProperty("产品线id")
    @NotNull(message = "产品线不能为空")
    private Long productLineId;

    @ApiModelProperty("影响数据指标")
    private String dataIndicators;

    @ApiModelProperty("目标客户/用户/项目")
    @NotBlank(message = "目标客户/用户/项目不能为空")
    private String targetCustomer;

    @ApiModelProperty("是否共创用户")
    @NotNull(message = "共创用户不能为空")
    private Boolean createCustomer;

    @ApiModelProperty("需求描述")
    @Length(max = 20000,message = "需求描述字数过大,请重新输入")
    private String desc;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipientInfoList;

    @ApiModelProperty("附件列表")
    private List<FileAddReq> fileList;

    @ApiModelProperty("线上bug id")
    private Long bugOnlineId;

    @ApiModelProperty("线下bug id")
    private Long bugOfflineId;

    @ApiModelProperty("需求接收人")
    private String receiveMan;

    @ApiModelProperty("需求接收人id")
    private String receiveManId;

    @ApiModelProperty("需求提交人")
    private String submitMan;

    @ApiModelProperty("需求提交人id")
    private String submitManId;

    @ApiModelProperty("是否客开需求")
    @NotNull(message = "是否客开需求")
    private Boolean customerDevDemand;

    @ApiModelProperty("客开类型：0基于销售合同；1基于项目")
    private Integer customerDevType;

    @ApiModelProperty("销售合同编号")
    private String customerDevSaleContract ;

    @ApiModelProperty("项目名称")
    private String customerDevProjectName;

    @ApiModelProperty("来源id")
    private String sourceId;

    @ApiModelProperty("业务id")
    private String bizId;

    @ApiModelProperty("业务数据名称")
    private String bizName;

    @ApiModelProperty("客户信息")
    private List<BizDemandCustomAddReq> customList;

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

    @ApiModelProperty("期望上线日期")
    private Date hopeReleaseDay;

    @ApiModelProperty("SR专家")
    private String srExpert = "";

    @ApiModelProperty("SR专家id")
    private String srExpertId = "";

    @ApiModelProperty("是否影响客户订单")
    private Boolean affectCustomerOrder;

    @ApiModelProperty("卡单说明")
    @Length(max = 500, message = "卡单说明不能超过500字")
    private String stuckOrderInstructions;

    @ApiModelProperty("客户等级")
    private String customerGrade;
}
