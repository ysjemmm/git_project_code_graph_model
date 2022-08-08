package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/17 10:39
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug列表")
public class BugOnlineVO extends ToString {
    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("bug标题")
    private String name;

    @ApiModelProperty("bug状态：0问题上报，1待确认，2关闭，3问题确认，4问题修复，5QA修复确认，6待上线，7挂起，8完成，9已转需求'")
    private Integer status;

    @ApiModelProperty("bug状态")
    private String statusName;

    @ApiModelProperty("产品线")
    private List<String> productLineNameList;

    @ApiModelProperty("业务域")
    private List<String> bizDomainNameList;

    @ApiModelProperty("bug优先级：0低，1中，2高，3紧急")
    private Integer priority;

    @ApiModelProperty("bug优先级")
    private String priorityName;

    @ApiModelProperty("提出人")
    private String proposer;

    @ApiModelProperty("提出人id")
    private String proposerId;

    @ApiModelProperty("经办人")
    private String operator;

    @ApiModelProperty("经办人id")
    private String operatorId;

    @ApiModelProperty("bug原因")
    private Integer reason;

    @ApiModelProperty("bug原因-描述")
    private String reasonName;

    @ApiModelProperty("驳回原因")
    private Integer dismissCause;

    @ApiModelProperty("驳回原因-描述")
    private String dismissCauseName;

    @ApiModelProperty("bug环境：0生产环境，1模拟环境")
    private Integer env;

    @ApiModelProperty("bug环境")
    private String envName;

    @ApiModelProperty("bug所属端：0后端bug，1PC客户端，2PCweb端，3Android，4IOS，5H5")
    private Integer belong;

    @ApiModelProperty("bug所属端")
    private String belongName;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("预计上线日期")
    private Date expectLaunchDate;

    @ApiModelProperty("标签名称")
    private List<String> labelNames;
}