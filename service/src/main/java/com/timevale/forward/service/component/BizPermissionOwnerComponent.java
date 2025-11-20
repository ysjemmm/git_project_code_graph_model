package com.timevale.forward.service.component;

import com.timevale.forward.dal.entity.BizPermissionOwnerDO;
import com.timevale.forward.service.constant.BizPermissionScopeEnum;
import com.timevale.forward.service.constant.BizPermissionTypeEnum;
import lombok.NonNull;

import java.util.*;

/**
 * @description:
 * @author: mayang
 * @date: 2025/11/20 09:57
 */
public interface BizPermissionOwnerComponent {

    List<BizPermissionOwnerDO> getProductDemandOwnerList(@NonNull BizPermissionTypeEnum permissionType,
                                                                @NonNull BizPermissionScopeEnum permissionScope,
                                                                @NonNull Long scopeBizId);

    List<BizPermissionOwnerDO> getProductDemandOwnerList(@NonNull BizPermissionTypeEnum permissionType,
                                                                @NonNull BizPermissionScopeEnum permissionScope,
                                                                @NonNull Collection<Long> scopeBizIds);
}
