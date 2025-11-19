package com.timevale.forward.service;

import com.timevale.forward.dal.entity.BizPermissionOwnerDO;
import lombok.NonNull;

import java.util.Collection;
import java.util.List;

public interface BizPermissionOwnerService {

    /**
     * 校验权限
     *
     * @param ownerId           权限拥有者id
     * @param permissionType    权限类型
     * @param permissionScope   权限范围
     * @return
     */
    boolean checkPermission (@NonNull String ownerId, long permissionType, @NonNull String permissionScope, @NonNull Long scopeBizId);

    /**
     * 获取权限拥有者
     *
     * @param ownerId           权限拥有者id
     * @param permissionType    权限类型
     * @param permissionScope   权限范围
     * @param scopeBizIds       作用域业务id
     * @return
     */
    List<BizPermissionOwnerDO> listPermissionOwners (String ownerId, Long permissionType, String permissionScope, Collection<Long> scopeBizIds);

    /**
     * 添加权限
     *
     * @param bizPermissionOwnerDOs
     */
    void addPermissions (@NonNull Collection<BizPermissionOwnerDO> bizPermissionOwnerDOs);

    /**
     * 添加权限
     *
     * @param bizPermissionOwnerDOs
     * @param fillAllPermission    是否填充权限范围和权限类型
     */
    void addPermissionsIfNecessary (@NonNull Collection<BizPermissionOwnerDO> bizPermissionOwnerDOs, boolean fillAllPermission);

    /**
     * 添加权限
     *
     * @param permissionType
     * @param permissionScope
     * @param bizScopeBizId
     * @param onlyOwners
     */
    void upsertPermissions (@NonNull Long permissionType, @NonNull String permissionScope, @NonNull Long bizScopeBizId, Collection<BizPermissionOwnerDO> onlyOwners);

    /**
     * 删除权限
     *
     * @param permissionType
     * @param permissionScope
     * @param ownerIds
     */
    void deletePermissions (@NonNull Long permissionType, @NonNull String permissionScope, @NonNull Collection<String> ownerIds);

    /**
     * 删除权限
     *
     * @param ids
     */
    void deletePermissions (@NonNull Collection<Long> ids);

}
