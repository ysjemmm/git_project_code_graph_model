package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOfflineDO extends BaseDO {
    /**
     * 名称
     */
    private String name;

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 产品线id
     */
    private Long productLineId;

    /**
     * 状态 0bug打开、1待修复、2待验收、3待确认、4延期修复、5完成、6关闭
     */
    private Integer status;

    /**
     * 上一环节(bug打开或待修复)经办人
     */
    private String lastOperator;

    /**
     * 上一环节(bug打开或待修复)经办人id
     */
    private String lastOperatorId;

    /**
     * 经办人
     */
    private String operator;

    /**
     * 经办人花名拼音
     */
    private String operatorId;

    /**
     * bug提出人
     */
    private String proposer;

    /**
     * bug提出人id
     */
    private String proposerId;

    /**
     * bug优先级: 0紧急,10高,20中,30低
     */
    private Integer priority;

    /**
     * bug环境
     */
    private Integer env;

    /**
     * bug原因不能为空
     */
    private String reason;

    /**
     * bug来源不能为空
     */
    private Integer source;

    /**
     * bug所属端
     */
    private Integer belong;

    /**
     * 复现频率
     */
    private Integer frequency;

    /**
     * 描述
     */
    private String desc;

    /**
     * 打开次数
     */
    private Integer openCount;

    /**
     * 返回次数
     */
    private Integer returnCount;

    /**
     * 延期修复原因
     */
    private String delayHandleReason;

    /**
     * 不用修复原因:0被否定,1重复提交,2无法再次复现,3前端缓存,4产品需求调整,10无
     * */
    private Integer unHandleReason;

}
