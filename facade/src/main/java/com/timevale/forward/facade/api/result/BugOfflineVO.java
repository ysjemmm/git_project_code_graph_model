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
 * @date 2022/02/23 18:23
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线下bug列表")
public class BugOfflineVO extends ToString {
    
    @ApiModelProperty("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @ApiModelProperty("bug名称")
    private String bugName;

    @ApiModelProperty("项目名称")
    private String projectName;

    @ApiModelProperty("项目状态:0待启动,10规划中,20研发中,30测试中,40已发布,-10已暂停,-20已作废")
    private Integer projectStatus;

    @ApiModelProperty(value = "状态:0bug打开、1待修复、2待验收、3待确认、4延期修复、5完成、6关闭")
    private Integer status;

    @ApiModelProperty(value = "bug状态-描述")
    private String statusName;

    @ApiModelProperty(value = "优先级:0紧急,10高,20中,30低")
    private Integer priority;

    @ApiModelProperty(value = "优先级-描述")
    private String priorityName;

    @ApiModelProperty(value = "bug来源:0预演bug,1测试阶段bug,2历史版本bug,3自动化脚本执行发现bug,4代码review,5冒烟用例")
    private Integer source;

    @ApiModelProperty(value = "bug来源-描述")
    private String sourceName;

    @ApiModelProperty("bug原因：1功能错误，2功能缺失，3改动波及，4参数校验错误，5历史遗留，6实现与需求不符，7配置错误，8环境部署，9页面格式错误，10文案提示，11UI和原型不一致，12数据问题，13需求问题，14兼容性问题，15交互体验，16交付文档错误，17优化建议，18性能问题，19安全问题，20数据库问题，21低级错误，22外部原因，23重复出现，24合并代码冲突")
    private Integer reason;

    @ApiModelProperty(value = "bug原因-描述")
    private String reasonName;

    @ApiModelProperty(value = "bug所属端:0后端bug,1PC客户端,2PCweb端,3Android,4IOS,5H5")
    private Integer belong;

    @ApiModelProperty(value = "bug所属端-描述")
    private String belongName;

    @ApiModelProperty(value = "bug环境:0项目环境,1测试环境,2模拟环境,3生产环境")
    private Integer env;

    @ApiModelProperty(value = "bug环境-描述")
    private String envName;

    @ApiModelProperty("经办人")
    private String operator;

    @ApiModelProperty("经办人id")
    private String operatorId;

    @ApiModelProperty("提出人")
    private String proposer;

    @ApiModelProperty("提出人id")
    private String proposerId;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("业务域名称")
    private String bizDomainName;

    @ApiModelProperty("产品线名称")
    private String productLineName;

    @ApiModelProperty("不用修复原因:0被否定,1重复提交,2无法再次复现,3前端缓存,4产品需求调整,10无")
    private Integer unHandleReason;

    @ApiModelProperty("不用修复原因-描述")
    private String unHandleReasonName;

    @ApiModelProperty("打回次数")
    private Integer returnCount;

    @ApiModelProperty("打开次数")
    private Integer openCount;

    @ApiModelProperty("预计解决完成日期")
    private Date expectSolveDate;

    @ApiModelProperty("标签名称")
    private List<BizLabelSimpleVO> labelNames;
}
