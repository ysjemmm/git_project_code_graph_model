package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.models.auth.In;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

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

    @ApiModelProperty("接收人信息")
    private PersonVO receiveManInfo;

    @ApiModelProperty("需求部门Id")
    private Long deptId;

    @ApiModelProperty("需求部门")
    private String deptName;

    @ApiModelProperty("是否为已删除部门：0-未删除;1-已删除")
    private Integer deptDeleteFlag;

    @ApiModelProperty("创建人信息")
    private PersonVO createManInfo;

    @ApiModelProperty("需求提交人")
    private String submitMan;

    @ApiModelProperty("需求提交人id")
    private String submitManId;
}
