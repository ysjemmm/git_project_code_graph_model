package com.timevale.forward.dal.condition;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.Date;

/**
 * @author jingchun
 * created on 2023/4/19
 */
@Getter
@Setter
@Accessors(chain = true)
public class BizDemandUpdateCondition {

    /**
     * 业务需求id列表
     */
    private Collection<Long> ids;

    /**
     * 业务需求状态
     */
    private Integer status;

    /**
     * 项目结束时间
     */
    private Date projectEndDate;
    private boolean projectEndDateNull;

}
