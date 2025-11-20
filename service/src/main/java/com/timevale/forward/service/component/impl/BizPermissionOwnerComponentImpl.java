package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.entity.BizPermissionOwnerDO;
import com.timevale.forward.service.BizPermissionOwnerService;
import com.timevale.forward.service.component.BizPermissionOwnerComponent;
import com.timevale.forward.service.constant.BizPermissionScopeEnum;
import com.timevale.forward.service.constant.BizPermissionTypeEnum;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: mayang
 * @date: 2025/11/20 09:57
 */
@Service
public class BizPermissionOwnerComponentImpl implements BizPermissionOwnerComponent {

    @Resource
    private BizPermissionOwnerService bizPermissionOwnerService;

    public List<BizPermissionOwnerDO> getProductDemandOwnerList(@NonNull BizPermissionTypeEnum permissionType,
                                                                @NonNull BizPermissionScopeEnum permissionScope,
                                                                @NonNull Long scopeBizId) {
        return getProductDemandOwnerList(permissionType, permissionScope, Collections.singletonList(scopeBizId));
    }

    public List<BizPermissionOwnerDO> getProductDemandOwnerList(@NonNull BizPermissionTypeEnum permissionType,
                                                                @NonNull BizPermissionScopeEnum permissionScope,
                                                                @NonNull Collection<Long> scopeBizIds) {
        Set<Long> real = scopeBizIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        return bizPermissionOwnerService.listPermissionOwners(null, permissionType.getValue(), permissionScope.name(), real);
    }

}
