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
public class BugOfflineListCondition {
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
    private List<String> operatorIds;

    /**
     * 提出人
     */
    private List<String> proposerIds;

    /**
     * 关联项目
     */
    private List<Long> projectIds;

    /**
     * 产品线
     */
    private List<Long> productLineIds;

    /**
     * 业务域
     */
    private List<Long> bizDomainIds;

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
     * 来源
     */
    private List<Integer> sources;

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
     * 修改时间有区间
     */
    private Date modifyDateRight;

    /**
     * 打回次数判断类型
     */
    private Integer returnCountType;

    /**
     * 打回次数
     */
    private Integer returnCount;

    /**
     * 重复打开次数判断类型
     */
    private Integer openCountType;

    /**
     * 重复打开次数
     */
    private Integer openCount;


    /**
     * 用于判断是否为"抄送我的需求"tab
     */
    private String copier;

    /**
     *包含的id
     */
    private List<Long> containIds;
}
