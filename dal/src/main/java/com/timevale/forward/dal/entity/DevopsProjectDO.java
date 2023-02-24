package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2023/02/24 15:49
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DevopsProjectDO extends BaseDO{
    private Long mainId;

    private String devopsProjectName;

    private String devopsProjectSign;

    private String appName;

    private String appType;

    private String appBranch;

    private String appDomain;

    private Boolean statFlag;
}