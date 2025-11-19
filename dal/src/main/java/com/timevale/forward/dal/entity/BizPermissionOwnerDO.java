package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * @description:
 * @author: mayang
 * @date: 2025/11/18 15:01
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class BizPermissionOwnerDO extends BaseDO {

    private Long permissionType;

    private String permissionScope;

    private String ownerId;

    private String owner;

    private Long scopeBizId;
}
