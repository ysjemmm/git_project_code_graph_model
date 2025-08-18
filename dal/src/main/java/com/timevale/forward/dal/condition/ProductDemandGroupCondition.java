package com.timevale.forward.dal.condition;

import com.timevale.mandarin.common.query.QueryBase;
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
public class ProductDemandGroupCondition extends QueryBase {

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 业务域id
     */
    private Long bizDomainId;

    /**
     * 父级产品线id
     */
    private Long productLineId;

    /**
     * 子产品线id
     */
    private Long subProductLineId;

    /**
     * 状态:0待排期,10已列入项目,20 项目进行中,30已完成上线,-10已暂停,-20已作废
     */
    private Integer status;

    /**
     *  负责人
     */
    private String ownerId;

    /**
     *  负责人集合
     */
    private List<String> notInOwnerIds;

    /**
     * 排期时间
     */
    private Date expectScheduleTime;

    /**
     * 标签
     */
    private List<Long> labelIds;

    /**
     * 标签类别集合
     */
    private List<Long> labelCategoryIds;

    /**
     * 标签类别集合
     */
    private List<Long> notInLabelIds;

    /**
     * 需要包含的产品需求
     */
    private List<Long> inProductDemandIds;

    /**
     * 产品需求类型
     */
    private Integer type;
}
