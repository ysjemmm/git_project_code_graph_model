package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("任务查询条件")
public class TaskListCondition extends QueryBase {
    /**
     * id
     */
    private Long id;

    /**
     * id
     */
    private List<Long> ids;
    /**
     * 名称
     */
    @WildcardEscape
    private String name;

    /**
     * 产品线
     */
    private List<Long> projectIds;
    /**
     * 产品线
     */
    private List<Long> productLineIds;
    /**
     * 状态
     */
    private List<Integer> status;
    /**
     * 执行人
     */
    private List<String> executorIds;
    /**
     * 创建人
     */
    private List<String> createManIds;

    /**
     * 计划开始时间左区间
     */
    private Date planStartDateLeft;

    /**
     * 计划开始时间右区间
     */
    private Date planStartDateRight;

    /**
     * 计划结束时间左区间
     */
    private Date planEndDateLeft;

    /**
     * 计划结束时间右区间
     */
    private Date planEndDateRight;

    /**
     * 实际开始时间左区间
     */
    private Date actualStartDateLeft;

    /**
     * 实际开始时间右区间
     */
    private Date actualStartDateRight;

    /**
     * 实际结束时间左区间
     */
    private Date actualEndDateLeft;

    /**
     * 实际结束时间右区间
     */
    private Date actualEndDateRight;

}
