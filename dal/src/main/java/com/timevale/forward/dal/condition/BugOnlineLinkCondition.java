package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/12/23 14:23
 */
@Setter
@Getter
@Builder
public class BugOnlineLinkCondition {

    /**
     * 业务域id列表
     */
    private List<Long> bizDomainIdList;

    /**
     * 产品线id列表
     */
    private List<Long> productLineIdList;
}
