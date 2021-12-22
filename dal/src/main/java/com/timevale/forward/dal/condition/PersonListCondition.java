package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

/**
 * @author by YangXu
 * @date 2021/12/15 13:39
 */
@Data
@Builder
public class PersonListCondition {

    /**
     * 项目id
     */
    private Long projectId;

    /**
     * 产品需求id
     */
    private Long productDemandId;

    /**
     * 业务需求id
     */
    private Long bizDemandId;

    /**
     * 人员类型:0项目-产品经理，1项目-项目成员，20产品需求-抄送人，30业务需求-抄送人
     */
    private Integer type;
}
