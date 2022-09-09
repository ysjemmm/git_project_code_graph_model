package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @Date 2022/3/18 11:18
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOnlineDO extends BaseDO {
    /**
     * bug标题
     */
    private String name;

    /**
     * 经办人
     */
    private String operator;

    /**
     * 经办人id
     */
    private String operatorId;

    /**
     * 上一环节经办人
     */
    private String lastOperator;

    /**
     * 上一环节经办人id
     */
    private String lastOperatorId;

    /**
     * 提出人
     */
    private String proposer;

    /**
     * 提出人id
     */
    private String proposerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 业务需求id
     */
    private Long bizDemandId;

    /**
     * bug环境：0生产环境，1模拟环境
     */
    private Integer env;

    /**
     * bug所属端：0后端bug，1PC客户端，2PCweb端，3Android，4IOS，5H5
     */
    private Integer belong;

    /**
     * bug优先级：0低，1中，2高，3紧急
     */
    private Integer priority;

    /**
     * 是否复现：0是，1否
     */
    private Integer recurrent;

    /**
     * bug原因：0需求问题，1环境配置问题，2功能错误，3兼容性问题，4数据问题，5性能问题，6安全问题，
     * 7外部原因，8开发误操作，9接口文档编写错误，10外包项目，11历史版本，12无测试参与版本，13测试环境延期未修复bug，
     * 14设计缺陷，15定制版升级改动波及，16无法重现但客户环境偶现，17无法重现但客户环境必现
     */
    private Integer reason;

    /**
     * 期望解决日期
     */
    private Date expectDate;

    /**
     * 产品线相关业务，格式是json字符串格式
     */
    private String business;

    /**
     * bug详情描述
     */
    private String describe;

    /**
     * bug的上个状态：0问题上报，1待确认，2关闭，3问题确认，4问题修复，5QA修复确认，6待上线，7挂起，8完成，9已转需求
     */
    private Integer prevStatus;

    /**
     * 是否含挂起节点
     */
    private Boolean hangUp;

    /**
     * bug状态：0问题上报，1待确认，2关闭，3问题确认，4问题修复，5QA修复确认，6待上线，7挂起，8完成，9已转需求
     */
    private Integer status;

    /**
     * 驳回原因：0客户操作错误，1客户对业务理解错误，2产品不支持，3客户的回调地址错误，
     * 4客户对接版本错误，5配置套餐没有费用，6重复提交，7支行大额行号未配置，8实施传参错误，
     * 9网络波动，10客户自身缺陷，11实施给客户项目的配置错误，12实施对业务理解错误，
     * 13操作人录入错误，14需求变更，15历史数据未订正，16文档与实际不符，17长时间未反馈，
     * 18问题描述不清，19当前版本不支持，20可以升级版本解决，21报告人提供信息不全无法排查，
     * 22产品配置错误，23客户侧环境问题，24技术咨询
     */
    private Integer dismissCause;

    /**
     * 问题原因
     */
    private String problemReason;

    /**
     * 解决方案
     */
    private String solveScheme;

    /**
     * 修复失败原因
     */
    private String repairFailReason;

    /**
     * 重新打开原因
     */
    private String openAgainReason;

    /**
     * 系统菜单名称
     */
    private String systemMenuName;

    /**
     * bug来源:产研(forward),运营支撑(support)
     */
    private String source;

    /**
     * 模块id
     */
    private String modelId;

    /**
     * 详细版本号
     */
    private String detailVersionId;

    /**
     * 预计上线日期
     */
    private Date expectLaunchDate;

    /**
     * 关联的bug_id
     */
    private Long linkBugId;

    /**
     * 来源id(客开项目id或者合同id)
     */
    private String sourceId;

    /**
     * 来源名称
     */
    private String sourceName;

    /**
     * 工单id
     */
    private String bizId;

    /**
     * 工单标题
     */
    private String bizName;

}
