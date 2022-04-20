package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.*;
import lombok.Data;

import java.util.Date;

/**
 * @Date 2022/3/22 11:08
 * @Author 望轩
 */
@Data
public class BugOnlineMD extends BaseMD{
    /**
     * bug标题
     */
    @FieldCompare(fieldName = "bug标题")
    private String name;

    /**
     * 经办人
     */
    @FieldCompare(fieldName = "经办人")
    private String operator;

    /**
     * 提出人
     */
    @FieldCompare(fieldName = "提出人")
    private String proposer;

    /**
     * 客户名称
     */
    @FieldCompare(fieldName = "客户名称")
    private String customerName;

    /**
     * bug环境：0生产环境，1模拟环境
     */
    @FieldCompare(fieldName = "bug环境", enumClass = BugOnlineEnvEnum.class)
    private Integer env;

    /**
     * bug所属端：0后端bug，1PC客户端，2PCweb端，3Android，4IOS，5H5
     */
    @FieldCompare(fieldName = "bug所属端", enumClass = BugOnlineBeloneEnum.class)
    private Integer belong;

    /**
     * bug优先级：0低，1中，2高，3紧急
     */
    @FieldCompare(fieldName = "bug优先级", enumClass = BugOnlinePriorityEnum.class)
    private Integer priority;

    /**
     * 是否复现：0是，1否
     */
    @FieldCompare(fieldName = "能否复现", enumClass = BugOnlineRecurrentEnum.class)
    private Integer recurrent;

    /**
     * bug原因：0需求问题，1环境配置问题，2功能错误，3兼容性问题，4数据问题，5性能问题，6安全问题，
     * 7外部原因，8开发误操作，9接口文档编写错误，10外包项目，11历史版本，12无测试参与版本，13测试环境延期未修复bug，
     * 14设计缺陷，15定制版升级改动波及，16无法重现但客户环境偶现，17无法重现但客户环境必现
     */
    @FieldCompare(fieldName = "bug原因", enumClass = BugOnlineReasonEnum.class)
    private Integer reason;

    /**
     * 期望解决日期
     */
    @FieldCompare(fieldName = "期望解决日期")
    private Date expectDate;

    /**
     * 产品线相关业务，格式是json字符串格式
     */
    private String business;

    /**
     * bug详情描述
     */
    @FieldCompare(fieldName = "bug详情描述")
    private String describe;

    /**
     * 驳回原因：0客户操作错误，1客户对业务理解错误，2产品不支持，3客户的回调地址错误，
     * 4客户对接版本错误，5配置套餐没有费用，6重复提交，7支行大额行号未配置，8实施传参错误，
     * 9网络波动，10客户自身缺陷，11实施给客户项目的配置错误，12实施对业务理解错误，
     * 13操作人录入错误，14需求变更，15历史数据未订正，16文档与实际不符，17长时间未反馈，
     * 18问题描述不清，19当前版本不支持，20可以升级版本解决，21报告人提供信息不全无法排查，
     * 22产品配置错误，23客户侧环境问题，24技术咨询
     */
    @FieldCompare(fieldName = "驳回原因", enumClass = BugOnlineDismissCauseEnum.class)
    private Integer dismissCause;

    /**
     * 问题原因
     */
    @FieldCompare(fieldName = "问题原因")
    private String problemReason;

    /**
     * 解决方案
     */
    @FieldCompare(fieldName = "解决方案")
    private String solveScheme;

    /**
     * 修复失败原因
     */
    @FieldCompare(fieldName = "修复失败原因")
    private String repairFailReason;

    /**
     * 重新打开原因
     */
    @FieldCompare(fieldName = "重新打开原因")
    private String openAgainReason;
}