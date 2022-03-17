package com.timevale.forward.facade.api.result;

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

    @ApiModelProperty("bug原因")
    private String reasonName;

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

    @ApiModelProperty("bug状态：0问题上报，1待确认，2关闭，3问题确认，4问题修复，5QA修复确认，6待上线，7挂起，8完成，9已转需求'")
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

    @ApiModelProperty("关联业务需求")
    private BizDemandVO bizDemandVO;
}