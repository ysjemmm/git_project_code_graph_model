package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.timevale.forward.dal.dao.BizPermissionOwnerMapper;
import com.timevale.forward.dal.entity.BizPermissionOwnerDO;
import com.timevale.forward.service.BizPermissionOwnerService;
import com.timevale.forward.service.constant.BizPermissionScopeEnum;
import com.timevale.forward.service.constant.BizPermissionTypeEnum;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description: 提供一个统一的负责人权限校验入口
 * @author: mayang
 * @date: 2025/11/18 14:50
 */
@Service
public class BizPermissionOwnerServiceImpl implements BizPermissionOwnerService {

    @Resource
    private BizPermissionOwnerMapper bizPermissionOwnerMapper;

    public boolean checkPermission (@NonNull String ownerId, long permissionType, @NonNull String permissionScope, @NonNull Long scopeBizId) {
        List<BizPermissionOwnerDO> permissionOwners = bizPermissionOwnerMapper
                .listByPermissionTypeAndScopeAndOwnerIdIn(null, null, ownerId, Collections.singletonList(scopeBizId));
        if (permissionOwners.isEmpty()) {
            return false;
        }
        for (BizPermissionOwnerDO permissionOwner : permissionOwners) {
            // scope = ALL && type = ALL
            if (permissionScope.equalsIgnoreCase(BizPermissionScopeEnum.ALL.getName()) && permissionType == BizPermissionTypeEnum.ALL.getValue()) {
                return true;
            }
            // scope = ALL && type 精确匹配
            else if (permissionScope.equalsIgnoreCase(BizPermissionScopeEnum.ALL.getName()) && permissionType == permissionOwner.getPermissionType()) {
                return true;
            }
            // scope 精确匹配 && type = ALL
            else if (permissionScope.equalsIgnoreCase(permissionOwner.getPermissionScope()) && permissionType == BizPermissionTypeEnum.ALL.getValue()) {
                return true;
            }
            // scope 模糊匹配 && type 模糊匹配
            else if (permissionScope.equalsIgnoreCase(permissionOwner.getPermissionScope()) && permissionType == permissionOwner.getPermissionType()) {
                return true;
            }
        }
        return false;
    }
    public List<BizPermissionOwnerDO> listPermissionOwners (String ownerId, Long permissionType, String permissionScope, Collection<Long> scopeBizIds) {
        return bizPermissionOwnerMapper
                .listByPermissionTypeAndScopeAndOwnerIdIn(permissionType, permissionScope, ownerId, scopeBizIds);
    }


    public void addPermissions (@NonNull Collection<BizPermissionOwnerDO> bizPermissionOwnerDOs) {
        addPermissionsIfNecessary(bizPermissionOwnerDOs, false);
    }

    public void addPermissionsIfNecessary (@NonNull Collection<BizPermissionOwnerDO> bizPermissionOwnerDOs, boolean fillAllPermission) {
        if (fillAllPermission) {
            bizPermissionOwnerDOs.forEach(e -> {
                e.setPermissionType(ObjectUtil.defaultIfNull(e.getPermissionType(), BizPermissionTypeEnum.ALL.getValue()));
                e.setPermissionScope(ObjectUtil.defaultIfBlank(e.getPermissionScope(), BizPermissionScopeEnum.ALL.getName()));
                if (e.getPermissionScope().equalsIgnoreCase(BizPermissionScopeEnum.ALL.getName())) {
                    e.setScopeBizId(null);
                }
            });
        }
        bizPermissionOwnerMapper.batchInsert(bizPermissionOwnerDOs);
    }

    public void upsertPermissions (@NonNull Long permissionType, @NonNull String permissionScope, @NonNull Long bizScopeBizId, Collection<BizPermissionOwnerDO> onlyOwners) {
        List<BizPermissionOwnerDO> existedOwners = listPermissionOwners(null, permissionType, permissionScope, Collections.singletonList(bizScopeBizId));
        List<BizPermissionOwnerDO> real = CollUtil.defaultIfEmpty(onlyOwners, Collections.emptyList()).stream()
                .filter(Objects::nonNull)
                .filter(e -> Objects.nonNull(e.getOwnerId()) && StrUtil.isNotBlank(e.getOwner()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(existedOwners) && CollUtil.isEmpty(real)) {
            return;
        }
        Set<String> realKeys = real.stream().map(e -> e.getOwnerId() + "|" + e.getOwner()).collect(Collectors.toSet());
        Set<String> existedKeys = existedOwners.stream().map(e -> e.getOwnerId() + "|" + e.getOwner()).collect(Collectors.toSet());
        List<BizPermissionOwnerDO> toAdd = real.stream().filter(e -> !existedKeys.contains(e.getOwnerId() + "|" + e.getOwner()))
                .peek(e -> {
                    e.setScopeBizId(bizScopeBizId);
                    e.setPermissionScope(permissionScope);
                    e.setPermissionType(permissionType);
                }).collect(Collectors.toList());
        List<BizPermissionOwnerDO> toDelete = existedOwners.stream().filter(e -> !realKeys.contains(e.getOwnerId() + "|" + e.getOwner()))
                .peek(e -> {
                    e.setScopeBizId(bizScopeBizId);
                    e.setPermissionScope(permissionScope);
                    e.setPermissionType(permissionType);
                }).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(toAdd)) {
            addPermissions(toAdd);
        }
        if (CollUtil.isNotEmpty(toDelete)) {
            deletePermissions(permissionType, permissionScope, toDelete.stream().map(BizPermissionOwnerDO::getOwnerId).collect(Collectors.toSet()));
        }
    }

    public void deletePermissions (@NonNull Long permissionType, @NonNull String permissionScope, @NonNull Collection<String> ownerIds) {
        if (CollUtil.isEmpty(ownerIds)) {
            return;
        }
        bizPermissionOwnerMapper.batchDeletePermissionOwners(permissionType, permissionScope, ownerIds);
    }

    public void deletePermissions (@NonNull Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        bizPermissionOwnerMapper.batchDeleteByIds(ids.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
    }

}
