package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2022/04/25 10:10
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectRiskExplanationDO extends BaseDO {

    /**
     * 项目风险id
     */
    private Long projectRiskId;

    /**
     * 风险说明
     */
    private String explanation;

}
