package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
