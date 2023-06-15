package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;

/**
 * @Date 2022/3/17 14:57
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("线上bug编辑")
public class BugOnlineModifyReq extends ToString {
    @ApiModelProperty("线上bug的id")
    @NotNull(message = "线上bug的id不能为空")
    private Long id;

    @ApiModelProperty("不用修复原因：0客户操作错误，1客户对业务理解错误，2产品不支持，3客户的回调地址错误，4客户对接版本错误，5配置套餐没有费用，6重复提交，7支行大额行号未配置，8实施传参错误，9网络波动，10客户自身缺陷，11实施给客户项目的配置错误，12实施对业务理解错误，13操作人录入错误，14需求变更，15历史数据未订正，16文档与实际不符，17长时间未反馈，18问题描述不清，19当前版本不支持，20可以升级版本解决，21报告人提供信息不全无法排查，22产品配置错误，23客户侧环境问题，24技术咨询")
    private Integer dismissCause;

    @ApiModelProperty("问题原因")
    private String problemReason;

    @ApiModelProperty("bug修复方案")
    private String solveScheme;

    @ApiModelProperty("修复失败原因")
    private String repairFailReason;

    @ApiModelProperty("重新打开原因")
    private String openAgainReason;

    @ApiModelProperty("预计上线日期")
    private Date expectLaunchDate;

    @ApiModelProperty("关联的线上bug id")
    private Long linkBugId;

    @ApiModelProperty("线上bug日志最新创建时间")
    private Date bugLogLastCreateDate;

    @ApiModelProperty("标题")
    @NotNull(message = "标题不能为空")
    private String name;

    @ApiModelProperty("提出人:格式 花名-真名")
    @NotNull(message = "提出人不能为空")
    private String proposer;

    @ApiModelProperty("提出人花名拼音")
    @NotNull(message = "提出人花名拼音不能为空")
    private String proposerId;

    @ApiModelProperty("经办人:格式 花名-真名")
    @NotNull(message = "经办人不能为空")
    private String operator;

    @ApiModelProperty("经办人花名拼音")
    @NotNull(message = "经办人花名拼音不能为空")
    private String operatorId;

    @ApiModelProperty("客户名称")
    private String customerName;

    @ApiModelProperty(value = "产品线id")
    @NotNull(message = "产品线id不能为空")
    private List<Long> productLineIdList;

    @ApiModelProperty(value = "bug环境：0生产环境，1模拟环境")
    @NotNull(message = "bug环境不能为空")
    private Integer env;

    @ApiModelProperty(value = "bug所属端：0后端bug，1PC客户端，2PCweb端，3Android，4IOS，5H5")
    @NotNull(message = "bug所属端不能为空")
    private Integer belong;

    @ApiModelProperty(value = "bug优先级：0低，1中，2高，3紧急")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @ApiModelProperty("是否复现：0是，1否")
    private Integer recurrent;

    @ApiModelProperty(value = "bug原因：0需求问题，1环境配置问题，2功能错误，3兼容性问题，4数据问题，5性能问题，6安全问题，7外部原因，8开发误操作，9接口文档编写错误，10外包项目，11历史版本，12无测试参与版本，13测试环境延期未修复bug，14设计缺陷，15定制版升级改动波及，16无法重现但客户环境偶现，17无法重现但客户环境必现")
    private Integer reason;

    @ApiModelProperty("期望解决日期")
    private Date expectDate;

    @ApiModelProperty("产品线相关业务，格式是json字符串格式:{flowId->流程flow id,mainOId->实名主体 oid,templateId->模板id,appId->appid,sealId->sealid,operatorNameAccount->操作人姓名账号,loginAccount->登录账号}")
    private String business;

    @ApiModelProperty("bug详情描述")
    private String describe;

    @ApiModelProperty("文件信息")
    private List<FileAddReq> files;

    @ApiModelProperty("抄送人")
    private List<PersonAddReq> recipients;

    @ApiModelProperty("系统菜单名称")
    private String systemMenuName;

    @ApiModelProperty("来源: forward 产研系统， support 运营支撑平台， duty 值班反馈")
    @NotNull(message = "bug来源不能为空")
    private String source;

    @ApiModelProperty("模块id")
    private List<Long> modelIds;

    @ApiModelProperty("详细版本号")
    private String detailVersionId;

    @ApiModelProperty("来源数据id")
    private String sourceId;

    @ApiModelProperty("客开项目名称")
    private String customerDevProjectName;

    @ApiModelProperty("业务id")
    private String bizId;

    @ApiModelProperty("业务名称")
    private String bizName;

    @ApiModelProperty("标签id")
    private List<Long> labelIds;

    @ApiModelProperty("客户信息")
    private List<BugOnlineCustomAddReq> customList;

    @ApiModelProperty("0华南大区，1华北大区，2华东大区，3西部大区，9其他大区")
    private Integer area;

    @ApiModelProperty("问题类别:0-空,1-功能问题,2-性能问题,3-兼容性问题,4用户体验问题,5-安全问题")
    private Integer category;

    @ApiModelProperty("关联线下bug的id")
    private Long bugOfflineId;

    @ApiModelProperty("bug责任人列表")
    private List<PersonAddReq> principalList;

    @ApiModelProperty("归因阶段")
    private Integer reasonStage;

    @ApiModelProperty("不用修复原因归因阶段")
    private Integer dismissCauseStage;

    @ApiModelProperty("客户等级")
    private String customerGrade;

    @ApiModelProperty("用户临时解决方案")
    private String temporarySolution;

    @ApiModelProperty("客户数: 1-单客户; 2-2家或2家以上客户")
    private Integer customerCount;

    @ApiModelProperty("用户数: 1-1~2个; 2-3个或3个以上")
    private Integer userCount;

    @ApiModelProperty("问题发生时长: 1-24小时以内、; 2-24~72小时; 3-72小时以上")
    private Integer problemOccurredTime;
}
















