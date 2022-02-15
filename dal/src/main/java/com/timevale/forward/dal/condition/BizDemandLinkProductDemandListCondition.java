package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/23 11:38
 */
@Data
@Builder
public class BizDemandLinkProductDemandListCondition {

    /**
     * 产品需求主题
     */
    @WildcardEscape
    private String name;

    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 业务需求id
     */
    private Long bizDemandId;

    /**
     * 优先级： 0-紧急，10-高，20-中，30低
     */
    private List<Integer> priorityList;

    /**
     * 业务域id列表
     */
    private List<Long> bizDomainIdList;

    /**
     * 产品线id列表
     */
    private List<Long> productLineIdList;

    /**
     * 产品需求负责人id列表
     */
    private List<String> ownerIdList;

    /**
     * 起始时间
     */
    private Date createDateStart;

    /**
     * 结束时间
     */
    private Date createDateEnd;

    /**
     * 产品需求状态
     */
    private Integer status;

    /**
     * 已经关联的id列表
     */
    private List<Long> linkedIdList;
}
