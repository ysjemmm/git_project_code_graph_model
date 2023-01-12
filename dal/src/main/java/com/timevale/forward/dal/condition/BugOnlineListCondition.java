package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;

import java.util.Date;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Data
@Builder
public class BugOnlineListCondition {
    /**
     * 名称
     */
    @WildcardEscape
    private String name;

    /**
     * 状态
     */
    private List<Integer> status;

    /**
     * 经办人
     */
    private List<String> operatorIdList;

    /**
     * 提出人
     */
    private List<String> proposerIdList;

    /**
     * 产品线
     */
    private List<Long> productLineIdList;

    /**
     * 业务域
     */
    private List<Long> bizDomainIdList;

    /**
     * 优先级
     */
    private List<Integer> priorities;

    /**
     * bug环境
     */
    private List<Integer> envs;

    /**
     * 原因
     */
    private List<String> reasons;

    /**
     * 驳回原因列表
     */
    private List<Integer> dismissCauseList;

    /**
     * 属于
     */
    private List<Integer> belongs;

    /**
     * 创建时间左区间
     */
    private Date createDateLeft;

    /**
     * 创建时间右区间
     */
    private Date createDateRight;

    /**
     * 修改时间左区间
     */
    private Date modifyDateLeft;

    /**
     * 修改时间右区间
     */
    private Date modifyDateRight;

    /**
     * 用于判断是否为"抄送我的需求"tab
     */
    private String copier;

    /**
     * 客户名称
     */
    @WildcardEscape
    private String customerName;

    /**
     * 来源列表
     */
    private List<String> sourceList;

    /**
     * 包含的id
     */
    private List<Long> containIds;

    /**
     * 不包含的id
     */
    private List<Long> exclusiveIds;

    /**
     * 模块id
     */
    private List<Long> modelIds;

    /**
     * 来源id
     */
    private String sourceId;

    /**
     * 0华南大区，1华北大区，2华东大区，3西部大区，9其他大区
     */
    private List<Integer> areas;

    /**
     * 是否是系统关闭的bug
     */
    private Boolean isSystemCloseBug;

    /**
     * 描述
     */
    @WildcardEscape
    private String describe;

}
