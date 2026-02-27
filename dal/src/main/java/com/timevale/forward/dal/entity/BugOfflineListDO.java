package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOfflineListDO extends BaseDO {
    /**
     * 名称
     */
    private String bugName;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 项目状态
     */
    private Integer projectStatus;

    /**
     * 产品线名称
     */
    private String productLineName;

    /**
     * 业务域名称
     */
    private String bizDomainName;

    /**
     * 状态 0bug打开、1待修复、2待验收、3待确认、4延期修复、5完成、6关闭
     */
    private Integer status;

    /**
     * 经办人
     */
    private String operator;

    /**
     * 经办人id
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
     * 紧急程度: 0P0,10P1,20P2,30P3
     */
    private Integer severity;

    /**
     * bug环境
     */
    private Integer env;

    /**
     * bug原因
     */
    private Integer reason;

    /**
     * bug来源不能为空
     */
    private Integer source;

    /**
     * bug所属端
     */
    private Integer belong;

    /**
     * 打回次数
     */
    private Integer returnCount;

    /**
     * 打开次数
     */
    private Integer openCount;

    /**
     * 不用修复原因
     */
    private Integer unHandleReason;

    /**
     * 预计解决完成日期
     */
    private Date expectSolveDate;

}
