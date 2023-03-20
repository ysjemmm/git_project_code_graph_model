package com.timevale.forward.facade.api.result;

import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/17 13:46
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug详情")
public class BugOnlineDetailVO extends ToString {

    @ApiModelProperty("id")
    private Long id;

    @ApiModelProperty("bug标题")
    private String name;

    @ApiModelProperty("提出人")
    private String proposer;

    @ApiModelProperty("提出人id")
    private String proposerId;

    @ApiModelProperty("经办人")
    private String operator;

    @ApiModelProperty("经办人id")
    private String operatorId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty("产品线")
    private List<ProductLineVO> productLineVOList;

    @ApiModelProperty("bug环境：0生产环境，1模拟环境")
    private Integer env;

    @ApiModelProperty("bug环境")
    private String envName;

    @ApiModelProperty("bug所属端：0后端bug，1PC客户端，2PCweb端，3Android，4IOS，5H5")
    private Integer belong;

    @ApiModelProperty("bug所属端")
    private String belongName;

    @ApiModelProperty("bug优先级：0低，1中，2高，3紧急")
    private Integer priority;

    @ApiModelProperty("bug优先级")
    private String priorityName;

    @ApiModelProperty("bug原因")
    private Integer reason;

    @ApiModelProperty("bug原因：0需求问题，1环境配置问题，2功能错误，3兼容性问题，4数据问题，5性能问题" +
            "，6安全问题，7外部原因，8开发误操作，9接口文档编写错误，10外包项目，11历史版本，12无测试参与版本" +
            "，13测试环境延期未修复bug，14设计缺陷，15定制版升级改动波及，16无法重现但客户环境偶现" +
            "，17无法重现但客户环境必现")
    private String reasonName;

    @ApiModelProperty("来源: forward 产研系统， support 运营支撑平台， duty 值班反馈")
    private String source;

    @ApiModelProperty("来源数据id")
    private String sourceId;

    @ApiModelProperty("来源-描述")
    private String sourceName;

    @ApiModelProperty("项目名称")
    private String customerDevProjectName;

    @ApiModelProperty("是否复现：0是，1否")
    private Integer recurrent;

    @ApiModelProperty("是否复现")
    private String recurrentName;

    @ApiModelProperty("期望解决日期")
    private Date expectDate;

    @ApiModelProperty("产品线相关业务，格式是json字符串格式")
    private String business;

    @ApiModelProperty("bug详情描述")
    private String describe;

    @ApiModelProperty("附件集合")
    private List<FileVO> files;

    @ApiModelProperty("抄送人")
    private List<PersonVO> recipientInfoList;

    @ApiModelProperty("评论")
    private List<CommentVO> commentVOList;

    @ApiModelProperty("创建时间")
    private Date createDate;

    @ApiModelProperty("更新时间")
    private Date modifyDate;

    @ApiModelProperty("bug上次状态：0问题上报，1待确认，2关闭，3问题确认，4问题修复，5QA修复确认，6待上线，7挂起，8完成，9已转需求")
    private Integer prevStatus;

    @ApiModelProperty("是否含挂起节点")
    private Boolean hangUp;

    @ApiModelProperty("bug状态：0问题上报，1待确认，2关闭，3问题确认，4问题修复，5QA修复确认，6待上线，7挂起，8完成，9已转需求")
    private Integer status;

    @ApiModelProperty("bug状态")
    private String statusName;

    @ApiModelProperty("驳回原因")
    private Integer dismissCause;

    @ApiModelProperty("驳回原因")
    private String dismissCauseName;

    @ApiModelProperty("重新打开原因")
    private String openAgainReason;

    @ApiModelProperty("问题原因")
    private String problemReason;

    @ApiModelProperty("解决方案")
    private String solveScheme;

    @ApiModelProperty("修复失败原因")
    private String repairFailReason;

    @ApiModelProperty("模块id")
    private List<Long> modelIds;

    @ApiModelProperty("模块名称")
    private String modelName;

    @ApiModelProperty("详细版本号")
    private String detailVersionId;

    @ApiModelProperty("预计上线日期")
    private Date expectLaunchDate;

    @ApiModelProperty("被关联bug")
    private List<BugOnlineLinkVO> linkedBugs;

    @ApiModelProperty("关联bug")
    private BugOnlineLinkVO linkBug;

    @ApiModelProperty("客户信息")
    private List<BugOnlineCustomVO> customList;

    @ApiModelProperty("0华南大区，1华北大区，2华东大区，3西部大区，9其他大区")
    private Integer area;

    @ApiModelProperty("线上bug日志最新创建时间")
    private Date bugLogLastCreateDate;

    @ApiModelProperty("业务需求列表")
    private List<BizDemandVO> bizDemands;

    @ApiModelProperty("客户等级")
    private String customerGrade;

    @ApiModelProperty("bug问题类别:0-空,1-功能问题,2-性能问题,3-兼容性问题,4用户体验问题,5-安全问题")
    private Integer category;

    @ApiModelProperty("bug问题类别:0-空,1-功能问题,2-性能问题,3-兼容性问题,4用户体验问题,5-安全问题")
    private String categoryName;

    @ApiModelProperty("关联线下bug的id")
    private Long bugOfflineId;

    @ApiModelProperty("关联线下bug的名称")
    private Long bugOfflineName;

    @ApiModelProperty("bug责任人列表")
    private List<PersonVO> principalList;

    @ApiModelProperty("归因阶段")
    private Integer reasonStage;

    @ApiModelProperty("归因阶段描述")
    private Integer reasonStageName;

    @ApiModelProperty("驳回原因归因阶段")
    private Integer dismissCauseStage;

    @ApiModelProperty("驳回原因归因阶段描述")
    private Integer dismissCauseStageName;
}