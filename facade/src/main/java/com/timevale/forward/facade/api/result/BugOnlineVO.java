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

    @ApiModelProperty("bug状态")
    private Integer status;

    @ApiModelProperty("bug状态描述")
    private String statusName;

    @ApiModelProperty("产品线")
    private List<String> productLineNameList;

    @ApiModelProperty("业务域")
    private List<String> bizDomainNameList;

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

    @ApiModelProperty("bug原因-描述")
    private String reasonName;

    @ApiModelProperty("不用修复原因-描述")
    private String dismissCauseName;

    @ApiModelProperty("来源数据id")
    private String sourceId;

    @ApiModelProperty("来源-描述")
    private String sourceName;

    @ApiModelProperty("bug环境")
    private String envName;

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
    private List<BizLabelSimpleVO> labelNames;

    @ApiModelProperty("模块名称")
    private List<String> modelNames;

    @ApiModelProperty("区域:0华南大区，1华北大区，2华东大区，3西部大区，9其他大区")
    private Integer area;

    @ApiModelProperty("详情描述")
    private String describe;

    @ApiModelProperty("原因阶段-描述")
    private String reasonStageName;

    @ApiModelProperty("不用修复原因阶段-描述")
    private String dismissCauseStageName;

    @ApiModelProperty("客户等级")
    private String customerGrade;

    @ApiModelProperty("问题类别:0-空,1-功能问题,2-性能问题,3-兼容性问题,4用户体验问题,5-安全问题")
    private String categoryName;

    @ApiModelProperty("打开次数")
    private Integer openCount;
}