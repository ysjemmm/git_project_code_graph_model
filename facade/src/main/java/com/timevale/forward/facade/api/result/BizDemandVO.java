package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("业务需求信息")
public class BizDemandVO extends ToString {

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

    @ApiModelProperty("业务域id")
    private String bizDomainId;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("需求部门Id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long deptId;

    @ApiModelProperty("需求部门名称")
    private String deptName;

    @ApiModelProperty("是否为已删除部门：0-未删除;1-已删除")
    private Integer deptDeleteFlag;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("预计上线时间0 (Q1上旬)，1(Q1中旬)，2 (Q1下旬)，3 (Q2上旬)，4 (Q2中旬)，5(Q2下旬)，6(Q3上旬)，7 (Q3中旬)，8 (Q3下旬)，6 (Q4上旬)，7 (Q4中旬)，8 (Q4下旬)，9暂无法评估")
    private Integer planReleaseDate;

    @ApiModelProperty("预计上线时间名称")
    private String planReleaseDateText;

    @ApiModelProperty("需求解决状态:0待评估，10已接收，20已列入项目，30项目进行中，40已完成上线，-10被驳回，-20已作废")
    private Integer status;

    @ApiModelProperty("需求解决状态名称")
    private String statusText;

    @ApiModelProperty("需求接收人")
    private String receiveMan;

    @ApiModelProperty("需求接收人id")
    private String receiveManId;

    @ApiModelProperty("需求提交人")
    private String submitMan;

    @ApiModelProperty("需求提交人id")
    private String submitManId;

    @ApiModelProperty("项目发布时间")
    private Date projectEndDate;

    @ApiModelProperty("标签名称")
    private List<BizLabelSimpleVO> labelNames;

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

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("期望上线时间0(1月),1(2月),2(3月),3(4月),4(5月),5(6月),7(8月),8(9月),9(10月),10(11月),11(12月)")
    private Integer hopeReleaseDate;

    @ApiModelProperty("期望上线时间名称")
    private String hopeReleaseDateText;

    @ApiModelProperty("期望上线日")
    private Date hopeReleaseDay;

    @ApiModelProperty("是否为客户开发项目:否,是")
    private String customerDevDemand;

    @ApiModelProperty("产品方案")
    private String productSolution;

    @ApiModelProperty("产研项目id")
    private Long projectId;

    @ApiModelProperty("产研项目名称")
    private String projectName;

    @ApiModelProperty("项目创建时间")
    private Date projectCreateDate;

}
