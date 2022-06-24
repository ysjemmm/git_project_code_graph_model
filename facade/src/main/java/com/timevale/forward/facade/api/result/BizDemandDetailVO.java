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
 * @author by YangXu
 * @date 2021/12/14 14:07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求详细信息")
public class BizDemandDetailVO extends ToString {

    @ApiModelProperty("业务需求id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("目标客户/用户/项目")
    private String targetCustomer;

    @ApiModelProperty("优先级： 0-紧急，10-高，20-中，30低")
    private Integer priority;

    @ApiModelProperty("优先级名称")
    private String priorityText;

    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("需求部门id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @ApiModelProperty("需求部门名称")
    private String deptName;

    @ApiModelProperty("是否为已删除部门：0-未删除;1-已删除")
    private Integer deptDeleteFlag;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("预计上线时间 12月份")
    private Integer planReleaseDate;

    @ApiModelProperty("预计上线时间名称")
    private String planReleaseDateText;

    @ApiModelProperty("需求解决状态:0待评估，10已接收，20已列入项目，30项目进行中，40已完成上线，-10被驳回，-20已作废")
    private Integer status;

    @ApiModelProperty("需求解决状态名称")
    private String statusText;

    @ApiModelProperty("影响数据指标")
    private String dataIndicators;

    @ApiModelProperty("是否共创用户")
    private Boolean createCustomer;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("附件")
    private List<FileVO> fileList;

    @ApiModelProperty("抄送人")
    private List<PersonVO> recipientInfoList;

    @ApiModelProperty("项目发布时间")
    private Date endDate;

    @ApiModelProperty("项目发布时间（新）")
    private Date projectEndDate;

    @ApiModelProperty("驳回理由")
    private Integer reason;

    @ApiModelProperty("驳回理由文本")
    private String reasonText;

    @ApiModelProperty("线上bug id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bugOnlineId;

    @ApiModelProperty("线上bug 名称")
    private String bugOnlineName;

    @ApiModelProperty("需求接收人")
    private String receiveMan;

    @ApiModelProperty("需求接收人id")
    private String receiveManId;

    @ApiModelProperty("需求提交人")
    private String submitMan;

    @ApiModelProperty("需求提交人id")
    private String submitManId;

    @ApiModelProperty("处理方案")
    private String solvePlan;

    @ApiModelProperty("拒绝原因")
    private String RejectReason;

    @ApiModelProperty("是否客开需求")
    private Boolean customerDevDemand;

    @ApiModelProperty("客开类型：0基于销售合同；1基于项目")
    private Integer customerDevType;

    @ApiModelProperty("销售合同编号")
    private String customerDevSaleContract ;

    @ApiModelProperty("项目名称")
    private String customerDevProjectName;
}
