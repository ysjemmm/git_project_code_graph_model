package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2022/03/22 11:08
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BugOnlineListDO extends BaseDO {
    /**
     * 名称
     */
    private String bugName;

    /**
     * 状态 0bug打开、1待修复、2待验收、3待确认、4延期修复、5完成、6关闭
     */
    private Integer status;

    /**
     * 产品线名称
     */
    private String productLineName;

    /**
     * 业务域名称
     */
    private String bizDomainName;

    /**
     * bug优先级: 0紧急,10高,20中,30低
     */
    private Integer priority;

    /**
     * bug提出人
     */
    private String proposer;

    /**
     * bug提出人id
     */
    private String proposerId;

    /**
     * 经办人
     */
    private String operator;

    /**
     * 经办人id
     */
    private String operatorId;

    /**
     * bug原因
     */
    private Integer reason;

    /**
     * bug环境
     */
    private Integer env;

    /**
     * bug所属端
     */
    private Integer belong;
}
