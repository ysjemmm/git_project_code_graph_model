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
 * @author: xingyun
 * @create: 2021-12-13 13:53
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug列表")
public class BugOfflineVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("projectId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long projectId;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty(value = "状态:0bug打开、1待修复、2待验收、3待确认、4延期修复、5完成、6关闭")
    private Integer status;

    @ApiModelProperty(value = "bug状态")
    private String statusName;

    @ApiModelProperty(value = "优先级:0紧急,10高,20中,30低")
    private Integer priority;

    @ApiModelProperty(value = "优先级")
    private String priorityName;

    @ApiModelProperty(value = "bug来源:0预演bug,1测试阶段bug,2历史版本bug,3自动化脚本执行发现bug")
    private Integer source;

    @ApiModelProperty(value = "bug来源")
    private String sourceName;

    @ApiModelProperty(value = "bug原因")
    private String reason;

    @ApiModelProperty(value = "bug所属端:0后端bug,1PC客户端,2PCweb端,3Android,4IOS,5H5")
    private Integer belong;

    @ApiModelProperty(value = "bug所属端")
    private String belongName;

    @ApiModelProperty(value = "bug环境:0项目环境,1测试环境,2模拟环境,3生产环境")
    private Integer env;

    @ApiModelProperty(value = "bug环境")
    private String envName;

    @ApiModelProperty("创建人id")
    private String createManId;
    
    @ApiModelProperty("创建人")
    private String createMan;

    @ApiModelProperty("经办人")
    private String operator;

    @ApiModelProperty("经办人id")
    private String operatorId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("产品线id")
    private Long productLineId;

    @ApiModelProperty("延期修复原因")
    private String delayHandleReason;

    @ApiModelProperty("不用修复原因:0被否定,1重复提交,2无法再次复现,3前端缓存,4产品需求调整,10无")
    private String unHandleReason;

}
