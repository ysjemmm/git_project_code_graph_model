package com.timevale.forward.facade.api.result;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/14 15:52
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("客户需求列表信息")
public class CustomDemandVO extends ToString {

    @ApiModelProperty("客户需求id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("需求主题")
    private String name;

    @ApiModelProperty("产品端")
    private Integer productEnd;

    @ApiModelProperty("产品端")
    private String productEndText;

    @ApiModelProperty("客户名称")
    private String customName;

    @ApiModelProperty("客户所在页面")
    private String page;

    @ApiModelProperty("所在页面链接")
    private String path;

    @ApiModelProperty("需求描述")
    private String desc;

    @ApiModelProperty("联系方式")
    private String contact;

    @ApiModelProperty("问题类别")
    private Integer cause;

    @ApiModelProperty("问题类别")
    private String causeText;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("需求解决状态:0待评估，10已接收，15已完成无需开发，20已列入项目，30项目进行中，40已完成上线，-10被驳回")
    private Integer status;

    @ApiModelProperty("需求解决状态")
    private String statusText;

    @ApiModelProperty("需求接收人")
    private String receiveMan;

    @ApiModelProperty("需求接收人id")
    private String receiveManId;

    @ApiModelProperty("需求提交人")
    private String submitMan;

    @ApiModelProperty("项目发布时间")
    private Date projectEndDate;

    @ApiModelProperty("驳回理由:0暂无法实现、1情绪不满、2定制需求")
    private String reasonText;

    @ApiModelProperty("驳回理由")
    private Integer reason;

    @ApiModelProperty("解决方案")
    private String solvePlan;
}
