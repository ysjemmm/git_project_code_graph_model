package com.timevale.forward.dal.entity;

import lombok.Getter;
import lombok.Setter;

/**
 * @author by YangXu
 * @date 2023/05/24 16:31
 */
@Getter
@Setter
public class ProjectBizDomainDO extends BaseDO {

    private Long projectId;

    private Long bizDomainId;
}