package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

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
     *包含的id
     */
    private List<Long> containIds;


}
